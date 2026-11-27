package com.revibe.backend.infrastructure.adapters.output.redis;

import com.revibe.backend.application.ports.output.security.SessionPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Sesiones en Redis:
 * <ul>
 * <li>session:{sha256(token)} -> userId, con TTL de 30 días que se renueva en cada uso.</li>
 * <li>user-sessions:{userId} -> set con los sha256 de sus tokens, para poder cerrarlas todas.</li>
 * </ul>
 * Se guarda el hash y no el token: si alguien lee Redis no obtiene tokens válidos. El set puede tener hashes de
 * sesiones ya caducadas; no molestan (borrar una clave que no existe no hace nada) y se limpian al crear sesiones.
 */
@Component
@RequiredArgsConstructor
public class RedisSessionAdapter implements SessionPort {

    private static final Duration TTL = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user-sessions:";

    /**
     * Lee el userId renovando el TTL de la sesión (GETEX) y renueva también el del índice del usuario: el índice tiene
     * que vivir al menos tanto como su sesión más reciente. Una sola ida y vuelta a Redis por petición.
     */
    private static final RedisScript<String> RESOLVE = new DefaultRedisScript<>("""
            local userId = redis.call('GETEX', KEYS[1], 'EX', ARGV[1])
            if userId then
              redis.call('EXPIRE', ARGV[2] .. userId, ARGV[1])
            end
            return userId
            """, String.class);

    private final StringRedisTemplate redis;

    @Override
    public String create(UUID userId) {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = sha256Hex(token);
        String index = indexKey(userId);

        pruneExpired(index);
        redis.opsForValue().set(SESSION_PREFIX + hash, userId.toString(), TTL);
        redis.opsForSet().add(index, hash);
        redis.expire(index, TTL);
        return token;
    }

    @Override
    public Optional<UUID> resolve(String token) {
        String userId = redis.execute(RESOLVE, List.of(SESSION_PREFIX + sha256Hex(token)),
                String.valueOf(TTL.toSeconds()), USER_SESSIONS_PREFIX);
        return Optional.ofNullable(userId).map(UUID::fromString);
    }

    @Override
    public void revoke(String token) {
        String hash = sha256Hex(token);
        String userId = redis.opsForValue().getAndDelete(SESSION_PREFIX + hash);
        if (userId != null) {
            redis.opsForSet().remove(USER_SESSIONS_PREFIX + userId, hash);
        }
    }

    @Override
    public void revokeAll(UUID userId) {
        String index = indexKey(userId);
        Set<String> hashes = redis.opsForSet().members(index);
        if (hashes != null && !hashes.isEmpty()) {
            redis.delete(hashes.stream().map(h -> SESSION_PREFIX + h).toList());
        }
        redis.delete(index);
    }

    @Override
    public void revokeOthers(UUID userId, String keepToken) {
        String keep = sha256Hex(keepToken);
        String index = indexKey(userId);
        Set<String> hashes = redis.opsForSet().members(index);
        if (hashes == null) {
            return;
        }
        List<String> others = new ArrayList<>();
        for (String hash : hashes) {
            if (!hash.equals(keep)) {
                others.add(hash);
            }
        }
        if (!others.isEmpty()) {
            redis.delete(others.stream().map(h -> SESSION_PREFIX + h).toList());
            redis.opsForSet().remove(index, others.toArray());
        }
    }

    @Override
    public Duration ttl() {
        return TTL;
    }

    /** Quita del índice las sesiones que ya han caducado solas, para que el set no crezca sin fin. */
    private void pruneExpired(String index) {
        Set<String> hashes = redis.opsForSet().members(index);
        if (hashes == null || hashes.isEmpty()) {
            return;
        }
        List<String> expired = new ArrayList<>();
        for (String hash : hashes) {
            if (!Boolean.TRUE.equals(redis.hasKey(SESSION_PREFIX + hash))) {
                expired.add(hash);
            }
        }
        if (!expired.isEmpty()) {
            redis.opsForSet().remove(index, expired.toArray());
        }
    }

    private static String indexKey(UUID userId) {
        return USER_SESSIONS_PREFIX + userId;
    }

    private static String sha256Hex(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.revibe.backend.infrastructure.adapters.output.redis;

import com.revibe.backend.application.ports.output.security.PasswordResetCodePort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * password-reset:{userId} -> hash { code: sha256(código), attempts: n }, con TTL. Un código activo por usuario: pedir
 * otro sustituye al anterior y pone los intentos a cero.
 */
@Component
@RequiredArgsConstructor
public class RedisPasswordResetCodeAdapter implements PasswordResetCodePort {

    static final Duration TTL = Duration.ofMinutes(15);
    /** 5 intentos sobre 1.000.000 de códigos posibles: 0,0005 % de acertar por código pedido. */
    static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final RedisScript<Long> CREATE = new DefaultRedisScript<>("""
            redis.call('DEL', KEYS[1])
            redis.call('HSET', KEYS[1], 'code', ARGV[1], 'attempts', 0)
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return 1
            """, Long.class);

    /** Comprobar y consumir (o sumar el intento fallido) de forma atómica: dos peticiones a la vez no se cuelan. */
    private static final RedisScript<Long> VERIFY = new DefaultRedisScript<>("""
            local stored = redis.call('HGET', KEYS[1], 'code')
            if not stored then
              return 0
            end
            if stored == ARGV[1] then
              redis.call('DEL', KEYS[1])
              return 1
            end
            local attempts = redis.call('HINCRBY', KEYS[1], 'attempts', 1)
            if attempts >= tonumber(ARGV[2]) then
              redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;

    @Override
    public String create(UUID userId) {
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        redis.execute(CREATE, List.of(key(userId)), sha256Hex(code), String.valueOf(TTL.toSeconds()));
        return code;
    }

    @Override
    public boolean verify(UUID userId, String code) {
        Long ok = redis.execute(VERIFY, List.of(key(userId)), sha256Hex(code), String.valueOf(MAX_ATTEMPTS));
        return ok != null && ok == 1;
    }

    private static String key(UUID userId) {
        return "password-reset:" + userId;
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

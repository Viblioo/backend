package com.revibe.backend.infrastructure.adapters.input.service;

import com.revibe.backend.application.ports.output.SessionPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionService implements SessionPort {

    private static final Duration TTL = Duration.ofDays(30);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final StringRedisTemplate redis;

    @Override
    public String create(UUID userId) {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(key(token), userId.toString(), TTL);
        return token;
    }

    public Optional<UUID> resolve(String token) {
        String k = key(token);
        String userId = redis.opsForValue().getAndExpire(k, TTL);
        return Optional.ofNullable(userId).map(UUID::fromString);
    }

    public void revoke(String token) {
        redis.delete(key(token));
    }

    private String key(String token) {
        return "session:" + sha256Hex(token);
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

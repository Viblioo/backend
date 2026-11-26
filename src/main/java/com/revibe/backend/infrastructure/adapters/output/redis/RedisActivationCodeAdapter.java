package com.revibe.backend.infrastructure.adapters.output.redis;

import com.revibe.backend.application.ports.output.ActivationCodePort;
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
import org.springframework.stereotype.Component;

/**
 * Guarda activation:{sha256(código)} -> userId con TTL. Así, si alguien lee Redis, no obtiene códigos usables.
 */
@Component
@RequiredArgsConstructor
public class RedisActivationCodeAdapter implements ActivationCodePort {

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    @Override
    public String create(UUID userId) {
        var bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(key(code), userId.toString(), TTL);
        return code;
    }

    @Override
    public Optional<UUID> consume(String code) {
        // getAndDelete es atómico: dos clics a la vez no pueden usar el mismo código
        String userId = redis.opsForValue().getAndDelete(key(code));
        return Optional.ofNullable(userId).map(UUID::fromString);
    }

    private static String key(String code) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            return "activation:" + HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.revibe.backend.infrastructure.adapters.output.redis;

import com.revibe.backend.application.ports.output.security.RateLimit;
import com.revibe.backend.application.ports.output.security.RateLimiterPort;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * ratelimit:{límite}:{clave} -> contador. La ventana empieza con el primer intento contado y dura
 * {@link RateLimit#window()}; al caducar la clave el contador vuelve a cero.
 */
@Component
@RequiredArgsConstructor
public class RedisRateLimiterAdapter implements RateLimiterPort {

    /**
     * INCR + EXPIRE en una sola operación atómica. Si se hicieran por separado y la app cayera entre medias, quedaría
     * un contador sin caducidad y la clave bloqueada para siempre.
     */
    private static final RedisScript<Long> INCREMENT_WITH_WINDOW = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;

    @Override
    public Optional<Duration> blockedFor(RateLimit limit, String key) {
        String redisKey = key(limit, key);
        String count = redis.opsForValue().get(redisKey);
        if (count == null || Long.parseLong(count) < limit.maxAttempts()) {
            return Optional.empty();
        }
        return Optional.of(remaining(limit, redisKey));
    }

    @Override
    public void recordFailure(RateLimit limit, String key) {
        increment(limit, key(limit, key));
    }

    @Override
    public void reset(RateLimit limit, String key) {
        redis.delete(key(limit, key));
    }

    @Override
    public Optional<Duration> tryAcquire(RateLimit limit, String key) {
        String redisKey = key(limit, key);
        long count = increment(limit, redisKey);
        return count > limit.maxAttempts() ? Optional.of(remaining(limit, redisKey)) : Optional.empty();
    }

    private long increment(RateLimit limit, String redisKey) {
        Long count = redis.execute(INCREMENT_WITH_WINDOW, List.of(redisKey),
                String.valueOf(limit.window().toSeconds()));
        return count == null ? 0 : count;
    }

    private Duration remaining(RateLimit limit, String redisKey) {
        Long seconds = redis.getExpire(redisKey);
        // -1 / -2 / null: sin TTL o caducada justo ahora; por seguridad, la ventana completa
        return seconds == null || seconds <= 0 ? limit.window() : Duration.ofSeconds(seconds);
    }

    private static String key(RateLimit limit, String key) {
        return "ratelimit:" + limit.name().toLowerCase(Locale.ROOT) + ":" + key;
    }
}

package com.revibe.backend.infrastructure.adapters.output.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.revibe.backend.application.ports.output.RateLimit;
import com.revibe.backend.support.IntegrationTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Los adaptadores de Redis contra el Redis real: los scripts Lua y los TTL solo se pueden probar de verdad así.
 */
class RedisAdaptersIntegrationTest extends IntegrationTest {

    @Autowired
    private RedisSessionAdapter sessions;
    @Autowired
    private RedisRateLimiterAdapter limiter;
    @Autowired
    private RedisPasswordResetCodeAdapter resetCodes;
    @Autowired
    private StringRedisTemplate redis;

    private final List<UUID> users = new ArrayList<>();
    private final List<String> limiterKeys = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        users.forEach(id -> {
            sessions.revokeAll(id);
            redis.delete("password-reset:" + id);
        });
        for (RateLimit limit : RateLimit.values()) {
            limiterKeys.forEach(key -> limiter.reset(limit, key));
        }
    }

    private UUID newUser() {
        var id = UUID.randomUUID();
        users.add(id);
        return id;
    }

    private String newLimiterKey() {
        var key = "test-" + UUID.randomUUID();
        limiterKeys.add(key);
        return key;
    }

    @Nested
    class Sessions {

        @Test
        void createdTokenResolvesToItsUser() {
            var userId = newUser();
            var token = sessions.create(userId);

            assertThat(sessions.resolve(token)).contains(userId);
            assertThat(sessions.resolve("token-inventado")).isEmpty();
        }

        @Test
        void eachUseRenewsTheTtl() {
            var userId = newUser();
            var token = sessions.create(userId);
            String hash = redis.opsForSet().members("user-sessions:" + userId).iterator().next();
            redis.expire("session:" + hash, Duration.ofSeconds(100));

            sessions.resolve(token);

            assertThat(redis.getExpire("session:" + hash)).isGreaterThan(Duration.ofDays(29).toSeconds());
            assertThat(redis.getExpire("user-sessions:" + userId)).isGreaterThan(Duration.ofDays(29).toSeconds());
        }

        @Test
        void tokenIsNotStoredInClear() {
            var userId = newUser();
            var token = sessions.create(userId);

            assertThat(redis.hasKey("session:" + token)).isFalse();
            assertThat(redis.opsForSet().members("user-sessions:" + userId)).doesNotContain(token);
        }

        @Test
        void revokeClosesOnlyThatSession() {
            var userId = newUser();
            var t1 = sessions.create(userId);
            var t2 = sessions.create(userId);

            sessions.revoke(t1);

            assertThat(sessions.resolve(t1)).isEmpty();
            assertThat(sessions.resolve(t2)).contains(userId);
            assertThat(redis.opsForSet().size("user-sessions:" + userId)).isEqualTo(1);
        }

        @Test
        void revokeAllClosesEverySessionOfTheUserAndNoOther() {
            var userId = newUser();
            var other = newUser();
            var t1 = sessions.create(userId);
            var t2 = sessions.create(userId);
            var otherToken = sessions.create(other);

            sessions.revokeAll(userId);

            assertThat(sessions.resolve(t1)).isEmpty();
            assertThat(sessions.resolve(t2)).isEmpty();
            assertThat(redis.hasKey("user-sessions:" + userId)).isFalse();
            assertThat(sessions.resolve(otherToken)).contains(other);
        }

        @Test
        void revokeOthersKeepsTheCurrentSession() {
            var userId = newUser();
            var t1 = sessions.create(userId);
            var current = sessions.create(userId);
            var t3 = sessions.create(userId);

            sessions.revokeOthers(userId, current);

            assertThat(sessions.resolve(current)).contains(userId);
            assertThat(sessions.resolve(t1)).isEmpty();
            assertThat(sessions.resolve(t3)).isEmpty();
            assertThat(redis.opsForSet().size("user-sessions:" + userId)).isEqualTo(1);
        }

        @Test
        void expiredSessionsArePrunedFromTheIndexOnNextLogin() {
            var userId = newUser();
            sessions.create(userId);
            String expiredHash = redis.opsForSet().members("user-sessions:" + userId).iterator().next();
            redis.delete("session:" + expiredHash); // como si hubiera caducado

            sessions.create(userId);

            assertThat(redis.opsForSet().members("user-sessions:" + userId))
                    .hasSize(1)
                    .doesNotContain(expiredHash);
        }
    }

    @Nested
    class RateLimiter {

        @Test
        void tryAcquireAllowsUpToTheMaxAndThenBlocksWithTheRemainingTime() {
            var limit = RateLimit.EMAIL_REQUESTS_PER_ADDRESS; // 3 por hora
            var key = newLimiterKey();

            for (int i = 0; i < limit.maxAttempts(); i++) {
                assertThat(limiter.tryAcquire(limit, key)).isEmpty();
            }
            var blocked = limiter.tryAcquire(limit, key);

            assertThat(blocked).isPresent();
            assertThat(blocked.get()).isPositive().isLessThanOrEqualTo(limit.window());
        }

        @Test
        void counterAlwaysHasATtl() {
            var limit = RateLimit.LOGIN_FAILURES_PER_ACCOUNT;
            var key = newLimiterKey();

            limiter.recordFailure(limit, key);

            assertThat(redis.getExpire("ratelimit:login_failures_per_account:" + key))
                    .isPositive()
                    .isLessThanOrEqualTo(limit.window().toSeconds());
        }

        @Test
        void blockedForOnlyAfterReachingTheMaxFailures() {
            var limit = RateLimit.LOGIN_FAILURES_PER_ACCOUNT; // 5
            var key = newLimiterKey();

            for (int i = 0; i < limit.maxAttempts() - 1; i++) {
                limiter.recordFailure(limit, key);
            }
            assertThat(limiter.blockedFor(limit, key)).isEmpty();

            limiter.recordFailure(limit, key);
            assertThat(limiter.blockedFor(limit, key)).isPresent();
        }

        @Test
        void resetUnblocks() {
            var limit = RateLimit.LOGIN_FAILURES_PER_ACCOUNT;
            var key = newLimiterKey();
            for (int i = 0; i < limit.maxAttempts(); i++) {
                limiter.recordFailure(limit, key);
            }

            limiter.reset(limit, key);

            assertThat(limiter.blockedFor(limit, key)).isEmpty();
        }

        @Test
        void differentLimitsWithTheSameKeyAreIndependent() {
            var key = newLimiterKey();
            for (int i = 0; i < RateLimit.LOGIN_FAILURES_PER_ACCOUNT.maxAttempts(); i++) {
                limiter.recordFailure(RateLimit.LOGIN_FAILURES_PER_ACCOUNT, key);
            }

            assertThat(limiter.blockedFor(RateLimit.CHANGE_PASSWORD_FAILURES_PER_USER, key)).isEmpty();
        }
    }

    @Nested
    class PasswordResetCodes {

        private static String wrong(String code) {
            return code.equals("000000") ? "111111" : "000000";
        }

        @Test
        void codeHasSixDigitsAndWorksOnce() {
            var userId = newUser();
            var code = resetCodes.create(userId);

            assertThat(code).matches("\\d{6}");
            assertThat(resetCodes.verify(userId, code)).isTrue();
            assertThat(resetCodes.verify(userId, code)).isFalse();
        }

        @Test
        void codeExpires() {
            var userId = newUser();
            resetCodes.create(userId);

            assertThat(redis.getExpire("password-reset:" + userId))
                    .isPositive()
                    .isLessThanOrEqualTo(RedisPasswordResetCodeAdapter.TTL.toSeconds());
        }

        @Test
        void correctCodeStillWorksBeforeReachingTheMaxAttempts() {
            var userId = newUser();
            var code = resetCodes.create(userId);

            for (int i = 0; i < RedisPasswordResetCodeAdapter.MAX_ATTEMPTS - 1; i++) {
                assertThat(resetCodes.verify(userId, wrong(code))).isFalse();
            }
            assertThat(resetCodes.verify(userId, code)).isTrue();
        }

        @Test
        void afterMaxFailedAttemptsEvenTheCorrectCodeIsRejected() {
            var userId = newUser();
            var code = resetCodes.create(userId);

            for (int i = 0; i < RedisPasswordResetCodeAdapter.MAX_ATTEMPTS; i++) {
                resetCodes.verify(userId, wrong(code));
            }

            assertThat(resetCodes.verify(userId, code)).isFalse();
        }

        @Test
        void askingForANewCodeInvalidatesThePreviousOne() {
            var userId = newUser();
            var first = resetCodes.create(userId);
            var second = resetCodes.create(userId);
            org.junit.jupiter.api.Assumptions.assumeFalse(first.equals(second)); // 1 entre 1.000.000

            assertThat(resetCodes.verify(userId, first)).isFalse();
            assertThat(resetCodes.verify(userId, second)).isTrue();
        }

        @Test
        void codeOfOneUserDoesNotWorkForAnother() {
            var userId = newUser();
            var other = newUser();
            var code = resetCodes.create(userId);
            resetCodes.create(other);

            assertThat(resetCodes.verify(other, code)).isFalse();
        }
    }
}

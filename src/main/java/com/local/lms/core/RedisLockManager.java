package com.local.lms.core;

import com.local.lms.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "app.lock.provider", havingValue = "redis")
@RequiredArgsConstructor
@Slf4j
public class RedisLockManager implements LockManager {

    private static final String LOCK_KEY_PREFIX = "lms:lock:";

    private final StringRedisTemplate redisTemplate;

    private String buildKey(String lockKey) {
        return LOCK_KEY_PREFIX + lockKey;
    }

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, action, 5_000L);
    }

    @Override
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    @Override
    public <T> T executeWithLock(String lockKey, Supplier<T> action, long timeoutMs) {
        String key = buildKey(lockKey);
        String value = Thread.currentThread().getName();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, value, timeoutMs, TimeUnit.MILLISECONDS);

        if (Boolean.FALSE.equals(acquired)) {
            throw new BusinessException("Could not acquire lock for key: " + lockKey
                    + " within " + timeoutMs + "ms — possible contention");
        }

        try {
            log.debug("Redis lock acquired for key={}", lockKey);
            return action.get();
        } finally {
            redisTemplate.delete(key);
            log.debug("Redis lock released for key={}", lockKey);
        }
    }
}

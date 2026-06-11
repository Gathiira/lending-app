package com.local.lms.core;

import com.local.lms.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
@Slf4j
public class EntityLockManager {

    // in case of multiple instance - replace this with distributed lock
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * Execute a supplier under a named lock.
     *
     * @param lockKey  unique key identifying the critical section (e.g. "loan:customerId:123")
     * @param action   the work to perform while holding the lock
     * @param <T>      return type
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock(true)); // fair lock
        lock.lock();
        try {
            log.debug("Lock acquired for key={}", lockKey);
            return action.get();
        } finally {
            lock.unlock();
            log.debug("Lock released for key={}", lockKey);
            // Optional: clean up if no threads are waiting
            lockMap.computeIfPresent(lockKey, (k, l) -> l.hasQueuedThreads() ? l : null);
        }
    }

    /**
     * Execute a runnable (void) under a named lock.
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Try to acquire a lock with a timeout — avoids indefinite blocking.
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action, long timeoutMs) {
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock(true));
        boolean acquired;
        try {
            acquired = lock.tryLock(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Lock acquisition interrupted for key: " + lockKey);
        }
        if (!acquired) {
            throw new BusinessException("Could not acquire lock for key: " + lockKey
                    + " within " + timeoutMs + "ms — possible contention or deadlock");
        }
        try {
            return action.get();
        } finally {
            lock.unlock();
            lockMap.computeIfPresent(lockKey, (k, l) -> l.hasQueuedThreads() ? l : null);
        }
    }
}
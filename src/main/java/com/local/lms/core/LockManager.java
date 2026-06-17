package com.local.lms.core;

import java.util.function.Supplier;

public interface LockManager {

    <T> T executeWithLock(String lockKey, Supplier<T> action);

    void executeWithLock(String lockKey, Runnable action);

    <T> T executeWithLock(String lockKey, Supplier<T> action, long timeoutMs);
}

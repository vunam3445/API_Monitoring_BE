package com.example.demo.modules.monitor.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Implement DistributedLockService bằng Redisson RLock.
 *
 * So với code thuần (SET NX EX), Redisson RLock có:
 * - Lock Ownership: chỉ thread nào acquire mới unlock được → tránh unlock nhầm.
 * - Watchdog (auto-extend): nếu không set leaseTime, Redisson tự gia hạn TTL
 *   mỗi 10s (default 30s TTL) → tránh lock hết hạn khi task chạy lâu.
 * - Reentrant: cùng 1 thread acquire nhiều lần không bị deadlock.
 * - Redlock support: khi dùng Redis cluster, tự động implement thuật toán Redlock.
 *
 * Key format: monitor:lock:{monitorId}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisDistributedLockService implements DistributedLockService {

    private static final String LOCK_PREFIX = "monitor:lock:";

    private final RedissonClient redissonClient;

    @Override
    public boolean tryLock(String monitorId, long ttlSeconds) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + monitorId);
        try {
            // waitTime = 0: không chờ, thử 1 lần rồi trả kết quả ngay
            // leaseTime = ttlSeconds: tự động unlock sau TTL (safety net)
            boolean acquired = lock.tryLock(0, ttlSeconds, TimeUnit.SECONDS);
            if (acquired) {
                log.debug("Lock acquired for monitor: {}", monitorId);
            } else {
                log.debug("Lock already held for monitor: {}", monitorId);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for monitor: {}", monitorId, e);
            return false;
        }
    }

    @Override
    public void unlock(String monitorId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + monitorId);
        try {
            // Sử dụng forceUnlock vì luồng lấy Lock (Scheduler) và luồng nhả Lock (Worker - RabbitMQ)
            // nằm trên 2 Thread (thậm chí 2 JVM / 2 máy khác nhau).
            // Do đó isHeldByCurrentThread() sẽ luôn trả về false và không bao giờ tự mở được khóa.
            boolean unlocked = lock.forceUnlock();
            if (unlocked) {
                log.debug("Lock successfully force-released for monitor: {}", monitorId);
            }
        } catch (Exception e) {
            log.error("Failed to release lock for monitor: {}", monitorId, e);
        }
    }
}

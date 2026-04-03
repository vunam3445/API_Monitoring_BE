package com.example.demo.modules.monitor.lock;

/**
 * Interface trừu tượng hóa cơ chế Distributed Lock.
 * Áp dụng Dependency Inversion Principle (DIP):
 * - Scheduler phụ thuộc vào abstraction, không phụ thuộc vào Redis cụ thể.
 * - Dễ dàng thay đổi implement (Redis → Zookeeper, DB-based lock...) mà không ảnh hưởng logic.
 */
public interface DistributedLockService {

    /**
     * Thử lấy lock cho một monitor cụ thể.
     *
     * @param monitorId ID của monitor cần lock
     * @param ttlSeconds Thời gian sống của lock (tự động mở khóa sau TTL)
     * @return true nếu lock thành công (chưa ai claim), false nếu đã bị lock bởi instance khác
     */
    boolean tryLock(String monitorId, long ttlSeconds);

    /**
     * Giải phóng lock cho một monitor sau khi hoàn thành xử lý.
     *
     * @param monitorId ID của monitor cần unlock
     */
    void unlock(String monitorId);
}

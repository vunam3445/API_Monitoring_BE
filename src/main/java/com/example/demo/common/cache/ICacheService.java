package com.example.demo.common.cache;

/**
 * Interface trừu tượng hóa các thao tác cache.
 * Tuân thủ nguyên lý SRP: tách logic cache ra khỏi business logic.
 * Tuân thủ nguyên lý DIP: các service phụ thuộc vào abstraction, không phụ thuộc vào Redis cụ thể.
 */
public interface ICacheService {

    /**
     * Lấy giá trị từ cache theo key.
     */
    Object get(String key);

    /**
     * Lưu giá trị vào cache với TTL (giây).
     */
    void set(String key, Object value, long ttlSeconds);

    /**
     * Xóa cache theo một key cụ thể.
     */
    void evict(String key);

    /**
     * Xóa tất cả cache key có prefix chỉ định.
     * Ví dụ: evictByPrefix("api-monitoring:api:list::") sẽ xóa tất cả key list.
     */
    void evictByPrefix(String prefix);
}

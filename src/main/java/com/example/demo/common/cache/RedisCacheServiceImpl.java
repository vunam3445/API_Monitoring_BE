package com.example.demo.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Triển khai ICacheService sử dụng RedisTemplate.
 * Đây là lớp duy nhất biết về Redis, các service khác chỉ phụ thuộc vào ICacheService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheServiceImpl implements ICacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Lỗi khi đọc cache key: {}", key, e);
            return null;
        }
    }

    @Override
    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Đã set cache key: {} với TTL: {}s", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Lỗi khi set cache key: {}", key, e);
        }
    }

    @Override
    public void evict(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("Xóa cache key: {} - kết quả: {}", key, deleted);
        } catch (Exception e) {
            log.error("Lỗi khi xóa cache key: {}", key, e);
        }
    }

    @Override
    public void evictByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Đã xóa {} cache key với prefix: {}", keys.size(), prefix);
            } else {
                log.info("Không tìm thấy cache key nào với prefix: {}", prefix);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xóa cache với prefix: {}", prefix, e);
        }
    }
}

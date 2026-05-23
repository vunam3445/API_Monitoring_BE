package com.example.demo.modules.dashboard.services;

import com.example.demo.common.cache.ICacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardCacheService {
    private final ICacheService cacheService;

    public void set(String key, Object value, long ttlSeconds) {
        cacheService.set(key, value, ttlSeconds);
    }

    public Object get(String key) {
        return cacheService.get(key);
    }

    public void evict(String key) {
        cacheService.evict(key);
    }

    public void clearUserDashboardCache(UUID userId) {
        // Clear all keys starting with dashboard: and containing the userId
        cacheService.evictByPrefix("dashboard:" + userId.toString() + ":");
    }

    public String buildKey(String type, UUID userId, String... extra) {
        StringBuilder sb = new StringBuilder("dashboard:")
                .append(userId.toString())
                .append(":")
                .append(type);
        for (String e : extra) {
            if (e != null) {
                sb.append(":").append(e);
            }
        }
        return sb.toString();
    }
}

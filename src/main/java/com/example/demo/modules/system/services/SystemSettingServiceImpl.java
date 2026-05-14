package com.example.demo.modules.system.services;

import com.example.demo.modules.system.entities.SystemSetting;
import com.example.demo.modules.system.repositories.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements ISystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    public static final String GLOBAL_PAUSE_KEY = "admin:global_pause";
    private static final String CACHE_SYSTEM_SETTINGS = "system:settings";

    @Cacheable(value = CACHE_SYSTEM_SETTINGS, key = "#key")
    public String getSetting(String key, String defaultValue) {
        return systemSettingRepository.findByKey(key)
                .map(SystemSetting::getValue)
                .orElse(defaultValue);
    }

    @Transactional
    @CacheEvict(value = CACHE_SYSTEM_SETTINGS, key = "#key")
    public void updateSetting(String key, String value, String description) {
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElse(SystemSetting.builder().key(key).build());
        
        setting.setValue(value);
        if (description != null) {
            setting.setDescription(description);
        }
        
        systemSettingRepository.save(setting);
    }

    public boolean isGlobalPaused() {
        return Boolean.parseBoolean(getSetting(GLOBAL_PAUSE_KEY, "false"));
    }

    public void setGlobalPause(boolean paused) {
        updateSetting(GLOBAL_PAUSE_KEY, String.valueOf(paused), "Trạng thái tạm dừng hệ thống toàn cục");
    }
}

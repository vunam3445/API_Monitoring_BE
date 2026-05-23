package com.example.demo.modules.system.services;

public interface ISystemSettingService {
    String getSetting(String key, String defaultValue);
    void updateSetting(String key, String value, String description);
    boolean isGlobalPaused();
    void setGlobalPause(boolean paused);
}

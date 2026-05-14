package com.example.demo.modules.system.services;

public interface IAdminSystemService {
    void flushMonitorQueue();
    void toggleGlobalPause(boolean paused);
    boolean isGlobalPaused();
}

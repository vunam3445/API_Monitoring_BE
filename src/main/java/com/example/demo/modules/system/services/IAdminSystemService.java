package com.example.demo.modules.system.services;

public interface IAdminSystemService {
    void flushMonitorQueue();
    void toggleGlobalPause(boolean paused);
    boolean isGlobalPaused();
    /** Số thread consumer RabbitMQ đang chạy thực tế (active listeners). */
    int getActiveWorkerCount();
    /** Tổng số thread consumer tối đa cấu hình trong properties. */
    int getTotalWorkerCount();
    /** % tải connection pool HikariCP (active / max * 100). */
    double getDbLoadPercent();
    /** Thời gian JVM đã chạy (ms). */
    long getServerUptimeMs();
    /** Số lượng message hiện đang nằm trong monitor execution queue. */
    int getQueueMessageCount();
}

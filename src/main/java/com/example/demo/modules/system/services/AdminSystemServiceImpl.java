package com.example.demo.modules.system.services;

import com.example.demo.modules.monitor.messaging.MonitorMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSystemServiceImpl implements IAdminSystemService {

    private final AmqpAdmin amqpAdmin;
    private final ISystemSettingService systemSettingService;

    /**
     * Xóa sạch hàng đợi các công việc monitor.
     * Dùng trong trường hợp tràn hàng đợi hoặc lỗi hàng loạt.
     */
    public void flushMonitorQueue() {
        log.info("Purging monitor execution queue: {}", MonitorMQConfig.QUEUE_NAME);
        amqpAdmin.purgeQueue(MonitorMQConfig.QUEUE_NAME, false);
    }

    /**
     * Tạm dừng hoặc tiếp tục việc giám sát toàn hệ thống.
     */
    public void toggleGlobalPause(boolean paused) {
        log.info("Setting global monitoring pause to: {}", paused);
        systemSettingService.setGlobalPause(paused);
    }

    public boolean isGlobalPaused() {
        return systemSettingService.isGlobalPaused();
    }
}

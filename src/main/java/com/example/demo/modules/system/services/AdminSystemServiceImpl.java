package com.example.demo.modules.system.services;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSystemServiceImpl implements IAdminSystemService {

    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;
    private final RabbitAdmin rabbitAdmin;
    private final DataSource dataSource;
    private final ActiveWorkerRegistry activeWorkerRegistry;

    @Value("${spring.rabbitmq.listener.simple.concurrency:20}")
    private int workerConcurrency;

    private boolean globalPaused = false;

    @Override
    public void flushMonitorQueue() {
        try {
            rabbitAdmin.purgeQueue("monitor.execution.queue", false);
            log.info("Flushed monitor.execution.queue");
        } catch (Exception e) {
            log.warn("Failed to flush monitor queue", e);
        }
    }

    @Override
    public void toggleGlobalPause(boolean paused) {
        this.globalPaused = paused;
        MessageListenerContainer monitorContainer = 
                rabbitListenerEndpointRegistry.getListenerContainer("monitorWorkerContainer");
        if (monitorContainer != null) {
            if (paused) {
                monitorContainer.stop();
                log.info("API Monitoring paused (monitor worker container stopped)");
            } else {
                monitorContainer.start();
                log.info("API Monitoring resumed (monitor worker container started)");
            }
        } else {
            log.warn("Monitor worker container not found for toggling pause status");
        }
    }

    @Override
    public boolean isGlobalPaused() {
        return this.globalPaused;
    }

    @Override
    public int getActiveWorkerCount() {
        // Trả về số Busy Threads thực tế từ registry thay vì đếm Alive Threads của Spring AMQP
        return activeWorkerRegistry.getActiveCount();
    }

    @Override
    public int getTotalWorkerCount() {
        try {
            int total = 0;
            for (MessageListenerContainer container : rabbitListenerEndpointRegistry.getListenerContainers()) {
                if (container instanceof SimpleMessageListenerContainer) {
                    try {
                        java.lang.reflect.Field field = SimpleMessageListenerContainer.class.getDeclaredField("concurrentConsumers");
                        field.setAccessible(true);
                        total += (int) field.get(container);
                    } catch (Exception e) {
                        log.warn("Failed to reflect concurrentConsumers field, fallback to default", e);
                        total += workerConcurrency;
                    }
                } else {
                    total += 1;
                }
            }
            return total > 0 ? total : workerConcurrency;
        } catch (Exception e) {
            log.warn("Failed to get total worker count", e);
            return workerConcurrency;
        }
    }

    @Override
    public double getDbLoadPercent() {
        try {
            if (dataSource.isWrapperFor(HikariDataSource.class)) {
                HikariDataSource hikari = dataSource.unwrap(HikariDataSource.class);
                int total = hikari.getMaximumPoolSize();
                int active = hikari.getHikariPoolMXBean() != null ? hikari.getHikariPoolMXBean().getActiveConnections() : 0;
                return total > 0 ? (double) active / total * 100 : 0.0;
            }
        } catch (Exception e) {
            log.warn("Failed to get DB load percentage", e);
        }
        return 0.0;
    }

    @Override
    public long getServerUptimeMs() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    @Override
    public int getQueueMessageCount() {
        try {
            Properties props = rabbitAdmin.getQueueProperties("monitor.execution.queue");
            if (props != null && props.get("QUEUE_MESSAGE_COUNT") != null) {
                Object count = props.get("QUEUE_MESSAGE_COUNT");
                return (count instanceof Number) ? ((Number) count).intValue() : 0;
            }
        } catch (Exception e) {
            log.warn("Failed to get queue message count", e);
        }
        return 0;
    }
}

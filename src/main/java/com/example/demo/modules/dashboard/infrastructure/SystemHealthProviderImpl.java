package com.example.demo.modules.dashboard.infrastructure;

import com.example.demo.modules.dashboard.domain.SystemHealthProvider;
import com.example.demo.modules.dashboard.dto.SystemHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemHealthProviderImpl implements SystemHealthProvider {

    private final MeterRegistry meterRegistry;
    private final RabbitAdmin rabbitAdmin;
    private final org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry endpointRegistry;

    @Override
    public SystemHealthResponse getSystemHealth() {
        // CPU usage (percentage)
        double cpuUsage = getCpuUsage();
        // RAM usage (percentage)
        double ramUsage = getRamUsage();
        // Disk usage (percentage) – assume root partition
        double diskUsage = getDiskUsage();
        // Pending messages in the monitoring queue (using monitor.execution.queue)
        int pending = getPendingQueueCount("monitor.execution.queue");
        // Check if all execution workers (listener containers) are running
        boolean workersRunning = areWorkersRunning();
        return SystemHealthResponse.builder()
                .cpuUsage(cpuUsage)
                .ramUsage(ramUsage)
                .diskUsage(diskUsage)
                .pendingQueue(pending)
                .isWorkersRunning(workersRunning)
                .build();
    }

    private boolean areWorkersRunning() {
        try {
            // If we have listeners, check if all of them are in RUNNING state.
            // In a typical setup, if any container is stopped, we consider workers not fully running.
            return !endpointRegistry.getListenerContainers().isEmpty() &&
                    endpointRegistry.getListenerContainers().stream()
                            .allMatch(org.springframework.amqp.rabbit.listener.MessageListenerContainer::isRunning);
        } catch (Exception e) {
            log.warn("Failed to check workers status", e);
            return false;
        }
    }

    private double getCpuUsage() {
        try {
            // Micrometer metric "process.cpu.usage" returns a value between 0 and 1 (fraction of a CPU).
            return meterRegistry.get("process.cpu.usage").gauge().value() * 100.0;
        } catch (Exception e) {
            log.warn("Unable to obtain CPU usage metric, fallback to OS MXBean", e);
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                double load = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
                return load >= 0 ? load * 100.0 : 0.0;
            }
            return 0.0;
        }
    }

    private double getRamUsage() {
        try {
            double used = meterRegistry.get("jvm.memory.used").tag("area", "heap").gauge().value();
            double max = meterRegistry.get("jvm.memory.max").tag("area", "heap").gauge().value();
            return max > 0 ? (used / max) * 100.0 : 0.0;
        } catch (Exception e) {
            log.warn("Unable to obtain RAM usage metric, fallback to Runtime", e);
            long total = Runtime.getRuntime().totalMemory();
            long free = Runtime.getRuntime().freeMemory();
            long used = total - free;
            return total > 0 ? ((double) used / total) * 100.0 : 0.0;
        }
    }

    private double getDiskUsage() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            long used = total - usable;
            return total > 0 ? ((double) used / total) * 100.0 : 0.0;
        } catch (Exception e) {
            log.warn("Unable to obtain disk usage", e);
            return 0.0;
        }
    }

    private int getPendingQueueCount(String queueName) {
        try {
            java.util.Properties props = rabbitAdmin.getQueueProperties(queueName);
            if (props == null) return 0;
            Object count = props.get("QUEUE_MESSAGE_COUNT");
            return (count instanceof Number) ? ((Number) count).intValue() : 0;
        } catch (Exception e) {
            log.warn("Failed to get pending queue count for {}", queueName, e);
            return 0;
        }
    }
}

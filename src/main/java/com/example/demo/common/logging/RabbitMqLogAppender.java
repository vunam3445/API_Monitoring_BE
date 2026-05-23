package com.example.demo.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Custom Logback Appender: Thu thập log lỗi nội bộ của ứng dụng Backend
 * (package com.example.demo) và đẩy vào RabbitMQ queue "system.logs.queue"
 * theo cơ chế bất đồng bộ (Async).
 *
 * Sử dụng RabbitMQ Java Client THUẦN (com.rabbitmq.client) thay vì Spring AMQP
 * để tránh lỗi Circular Dependency vì Logback được khởi tạo trước Spring Context.
 */
public class RabbitMqLogAppender extends AppenderBase<ILoggingEvent> {

    private Connection connection;
    private Channel channel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cấu hình mặc định — có thể override từ logback-spring.xml
    private String host = "localhost";
    private int port = 5672;
    private String username = "guest";
    private String password = "guest";
    private String queueName = "system.logs.queue";

    // Setters để Logback inject giá trị từ logback-spring.xml
    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setQueueName(String queueName) { this.queueName = queueName; }

    @Override
    public void start() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);

            this.connection = factory.newConnection();
            this.channel = connection.createChannel();
            // Đảm bảo queue tồn tại (durable = true)
            this.channel.queueDeclare(queueName, true, false, false, null);

            super.start();
        } catch (IOException | TimeoutException e) {
            addError("Failed to initialize RabbitMQ connection for ApplicationLog appender", e);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Chỉ xử lý log thuộc package nội bộ của dự án
        if (event.getLoggerName() == null || !event.getLoggerName().startsWith("com.example.demo")) {
            return;
        }

        try {
            Map<String, Object> logMsg = new HashMap<>();
            logMsg.put("timestamp", event.getTimeStamp());
            logMsg.put("level", event.getLevel().toString());
            logMsg.put("component", event.getLoggerName());
            logMsg.put("threadId", event.getThreadName());
            logMsg.put("message", event.getFormattedMessage());

            // Format StackTrace nếu log kèm theo Exception
            if (event.getThrowableProxy() != null) {
                logMsg.put("stackTrace", formatStackTrace(event.getThrowableProxy()));
            }

            byte[] body = objectMapper.writeValueAsBytes(logMsg);

            if (channel != null && channel.isOpen()) {
                channel.basicPublish("", queueName, null, body);
            }
        } catch (Exception e) {
            // In lỗi trực tiếp ra System.err để tránh vòng lặp log đệ quy vô hạn
            System.err.println("[RabbitMqLogAppender] Failed to send log to RabbitMQ: " + e.getMessage());
        }
    }

    private String formatStackTrace(IThrowableProxy throwableProxy) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwableProxy.getClassName())
                .append(": ")
                .append(throwableProxy.getMessage())
                .append("\n");

        StackTraceElementProxy[] elements = throwableProxy.getStackTraceElementProxyArray();
        if (elements != null) {
            for (StackTraceElementProxy step : elements) {
                sb.append("\tat ").append(step.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public void stop() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (IOException | TimeoutException e) {
            // Bỏ qua lỗi khi tắt appender
        }
        super.stop();
    }
}

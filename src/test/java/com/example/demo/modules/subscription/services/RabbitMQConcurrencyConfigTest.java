package com.example.demo.modules.subscription.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test xác thực các thuộc tính cấu hình concurrency của RabbitMQ
 * được nạp đúng vào Spring Context từ application.properties.
 *
 * Mục đích: Đảm bảo Enterprise Configurable Pattern hoạt động chính xác —
 * tức là các giá trị tùy chỉnh định nghĩa trong properties file được tiêm
 * đúng vào các @RabbitListener tương ứng khi khởi động ứng dụng.
 */
@SpringBootTest
public class RabbitMQConcurrencyConfigTest {

    @Value("${app.rabbitmq.concurrency.monitor}")
    private String monitorConcurrency;

    @Value("${app.rabbitmq.concurrency.log}")
    private String logConcurrency;

    @Value("${app.rabbitmq.concurrency.expiry}")
    private String expiryConcurrency;

    @Value("${app.rabbitmq.concurrency.broadcast}")
    private String broadcastConcurrency;

    @Test
    public void testConcurrencyPropertiesAreInjected() {
        assertThat(monitorConcurrency).isEqualTo("5-20");
        assertThat(logConcurrency).isEqualTo("2-4");
        assertThat(expiryConcurrency).isEqualTo("1-2");
        assertThat(broadcastConcurrency).isEqualTo("1-3");
    }
}

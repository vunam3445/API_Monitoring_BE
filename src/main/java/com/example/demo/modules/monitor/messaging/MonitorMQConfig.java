package com.example.demo.modules.monitor.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình RabbitMQ cho module Monitor.
 *
 * Topology:
 * - Exchange: monitor.exchange (Direct)
 * - Queue: monitor.execution.queue (durable)
 * - Routing Key: monitor.execute
 *
 * Luồng: Scheduler → Producer → Exchange → Queue → Worker (Consumer)
 */
@Configuration
public class MonitorMQConfig {

    public static final String EXCHANGE_NAME = "monitor.exchange";
    public static final String QUEUE_NAME = "monitor.execution.queue";
    public static final String ROUTING_KEY = "monitor.execute";

    /**
     * Direct Exchange: routing message dựa trên routing key chính xác.
     */
    @Bean
    public DirectExchange monitorExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    /**
     * Queue bền vững (durable) - không mất message khi RabbitMQ restart.
     */
    @Bean
    public Queue monitorExecutionQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    /**
     * Binding Queue với Exchange thông qua Routing Key.
     */
    @Bean
    public Binding monitorBinding(Queue monitorExecutionQueue, DirectExchange monitorExchange) {
        return BindingBuilder
                .bind(monitorExecutionQueue)
                .to(monitorExchange)
                .with(ROUTING_KEY);
    }

    /**
     * Sử dụng Jackson để serialize/deserialize message body thành JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate cấu hình sẵn JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}

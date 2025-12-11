package com.example.notificationservice.config;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitMQConfig {
    @Value("${rabbitmq.queue.reservation-created}")
    private String reservationCreatedQueue;
    @Value("${rabbitmq.queue.reservation-cancelled}")
    private String reservationCancelledQueue;
    @Value("${rabbitmq.queue.reservation-revoked}")
    private String reservationRevokedQueue;
    @Value("${rabbitmq.exchange.reservations}")
    private String reservationsExchange;
    @Bean
    public Queue reservationCreatedQueue() {
        return new Queue(reservationCreatedQueue, true);
    }
    @Bean
    public Queue reservationCancelledQueue() {
        return new Queue(reservationCancelledQueue, true);
    }
    @Bean
    public Queue reservationRevokedQueue() {
        return new Queue(reservationRevokedQueue, true);
    }
    @Bean
    public TopicExchange reservationsExchange() {
        return new TopicExchange(reservationsExchange);
    }
    @Bean
    public Binding reservationCreatedBinding() {
        return BindingBuilder
                .bind(reservationCreatedQueue())
                .to(reservationsExchange())
                .with("reservation.created");
    }
    @Bean
    public Binding reservationCancelledBinding() {
        return BindingBuilder
                .bind(reservationCancelledQueue())
                .to(reservationsExchange())
                .with("reservation.cancelled");
    }
    @Bean
    public Binding reservationRevokedBinding() {
        return BindingBuilder
                .bind(reservationRevokedQueue())
                .to(reservationsExchange())
                .with("reservation.revoked");
    }
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}

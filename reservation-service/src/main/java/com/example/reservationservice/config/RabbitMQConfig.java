package com.example.reservationservice.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    @Value("${rabbitmq.exchange.reservations}")
    private String reservationsExchange;

    @Bean
    public TopicExchange reservationsExchange() {
        return new TopicExchange(reservationsExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());

        // Enable publisher confirms
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                logger.info("Message published successfully. CorrelationId: {}", correlationData != null ? correlationData.getId() : "N/A");
            } else {
                logger.error("Failed to publish message. CorrelationId: {}, Cause: {}",
                        correlationData != null ? correlationData.getId() : "N/A", cause);
            }
        });

        // Enable mandatory flag for return callbacks
        template.setMandatory(true);
        template.setReturnsCallback(returned -> {
            logger.warn("Message returned. ReplyCode: {}, ReplyText: {}, Exchange: {}, RoutingKey: {}",
                    returned.getReplyCode(), returned.getReplyText(), returned.getExchange(), returned.getRoutingKey());
        });

        return template;
    }
}

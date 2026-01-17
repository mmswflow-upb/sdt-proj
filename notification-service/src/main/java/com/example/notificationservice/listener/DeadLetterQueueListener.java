package com.example.notificationservice.listener;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component
public class DeadLetterQueueListener {
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueListener.class);

    @RabbitListener(queues = "${rabbitmq.queue.dlq:reservation-dead-letter-queue}", ackMode = "MANUAL")
    public void handleDeadLetterMessage(String message,
                                        Channel channel,
                                        @Headers Map<String, ?> headers) {
        try {
            String correlationId = (String) headers.get("X-Correlation-ID");
            String originalRoutingKey = (String) headers.get("x-death");
            Integer redeliveryCount = getRedeliveryCount(headers);

            logger.error("=== MESSAGE IN DEAD LETTER QUEUE ===");
            logger.error("CorrelationId: {}", correlationId);
            logger.error("OriginalRoutingKey: {}", originalRoutingKey);
            logger.error("RedeliveryCount: {}", redeliveryCount);
            logger.error("Message Content: {}", message);
            logger.error("Headers: {}", headers);
            logger.error("=======================================");

            acknowledgeMessage(channel, headers);

            // Here you could add additional handling such as:
            // - Storing the message in a database for manual review
            // - Sending alerts/notifications
            // - Attempting recovery with exponential backoff
            // - Publishing to another queue for manual intervention
        } catch (Exception e) {
            logger.error("Error handling dead letter message: {}", e.getMessage(), e);
            rejectMessage(channel, headers);
        }
    }

    private Integer getRedeliveryCount(Map<String, ?> headers) {
        Object death = headers.get("x-death");
        if (death instanceof java.util.List) {
            java.util.List<?> deathList = (java.util.List<?>) death;
            if (!deathList.isEmpty()) {
                Object firstDeath = deathList.get(0);
                if (firstDeath instanceof java.util.Map) {
                    java.util.Map<?, ?> deathMap = (java.util.Map<?, ?>) firstDeath;
                    Object count = deathMap.get("count");
                    if (count instanceof Long) {
                        return ((Long) count).intValue();
                    } else if (count instanceof Integer) {
                        return (Integer) count;
                    }
                }
            }
        }
        return 0;
    }

    private void acknowledgeMessage(Channel channel, Map<String, ?> headers) {
        try {
            Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
            if (deliveryTag != null) {
                channel.basicAck(deliveryTag, false);
                logger.debug("Dead letter message acknowledged. DeliveryTag: {}", deliveryTag);
            }
        } catch (IOException e) {
            logger.error("Error acknowledging dead letter message", e);
        }
    }

    private void rejectMessage(Channel channel, Map<String, ?> headers) {
        try {
            Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
            if (deliveryTag != null) {
                channel.basicNack(deliveryTag, false, false);
                logger.warn("Dead letter message rejected and discarded. DeliveryTag: {}", deliveryTag);
            }
        } catch (IOException e) {
            logger.error("Error rejecting dead letter message", e);
        }
    }
}

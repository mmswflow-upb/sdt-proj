package com.example.notificationservice.listener;
import com.example.notificationservice.dto.ReservationNotificationMessage;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Map;

@Component
public class ReservationNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(ReservationNotificationListener.class);

    @RabbitListener(queues = "${rabbitmq.queue.reservation-created}", ackMode = "MANUAL")
    public void handleReservationCreated(@Payload ReservationNotificationMessage message,
                                         Channel channel,
                                         @Headers Map<String, ?> headers) {
        try {
            validateMessage(message);
            logger.info("=== RESERVATION CREATED NOTIFICATION ===");
            logger.info("Message Version: {}", message.getMessageVersion());
            logger.info("Event Type: {}", message.getEventType());
            logger.info("Reservation ID: {}", message.getReservationId());
            logger.info("User ID: {}", message.getUserId());
            logger.info("Room ID: {}", message.getRoomId());
            logger.info("Time: {} to {}", message.getStartDateTime(), message.getEndDateTime());
            logger.info("Status: {}", message.getStatus());
            logger.info("Timestamp: {}", message.getTimestamp());
            logger.info("========================================");
            acknowledgeMessage(channel, headers);
        } catch (Exception e) {
            handleError("RESERVATION_CREATED", message, headers, e);
            rejectMessage(channel, headers);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.reservation-cancelled}", ackMode = "MANUAL")
    public void handleReservationCancelled(@Payload ReservationNotificationMessage message,
                                           Channel channel,
                                           @Headers Map<String, ?> headers) {
        try {
            validateMessage(message);
            logger.info("=== RESERVATION CANCELLED NOTIFICATION ===");
            logger.info("Message Version: {}", message.getMessageVersion());
            logger.info("Event Type: {}", message.getEventType());
            logger.info("Reservation ID: {}", message.getReservationId());
            logger.info("User ID: {}", message.getUserId());
            logger.info("Room ID: {}", message.getRoomId());
            logger.info("Status: {}", message.getStatus());
            logger.info("Timestamp: {}", message.getTimestamp());
            logger.info("==========================================");
            acknowledgeMessage(channel, headers);
        } catch (Exception e) {
            handleError("RESERVATION_CANCELLED", message, headers, e);
            rejectMessage(channel, headers);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.reservation-revoked}", ackMode = "MANUAL")
    public void handleReservationRevoked(@Payload ReservationNotificationMessage message,
                                         Channel channel,
                                         @Headers Map<String, ?> headers) {
        try {
            validateMessage(message);
            logger.info("=== RESERVATION REVOKED NOTIFICATION ===");
            logger.info("Message Version: {}", message.getMessageVersion());
            logger.info("Event Type: {}", message.getEventType());
            logger.info("Reservation ID: {}", message.getReservationId());
            logger.info("User ID: {}", message.getUserId());
            logger.info("Room ID: {}", message.getRoomId());
            logger.info("Status: {}", message.getStatus());
            logger.info("Timestamp: {}", message.getTimestamp());
            logger.info("========================================");
            acknowledgeMessage(channel, headers);
        } catch (Exception e) {
            handleError("RESERVATION_REVOKED", message, headers, e);
            rejectMessage(channel, headers);
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.reservation-approved}", ackMode = "MANUAL")
    public void handleReservationApproved(@Payload ReservationNotificationMessage message,
                                          Channel channel,
                                          @Headers Map<String, ?> headers) {
        try {
            validateMessage(message);
            logger.info("=== RESERVATION APPROVED NOTIFICATION ===");
            logger.info("Message Version: {}", message.getMessageVersion());
            logger.info("Event Type: {}", message.getEventType());
            logger.info("Reservation ID: {}", message.getReservationId());
            logger.info("User ID: {}", message.getUserId());
            logger.info("Room ID: {}", message.getRoomId());
            logger.info("Status: {}", message.getStatus());
            logger.info("Timestamp: {}", message.getTimestamp());
            logger.info("=========================================");
            acknowledgeMessage(channel, headers);
        } catch (Exception e) {
            handleError("RESERVATION_APPROVED", message, headers, e);
            rejectMessage(channel, headers);
        }
    }

    private void validateMessage(ReservationNotificationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (message.getEventType() == null || message.getEventType().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (message.getReservationId() == null) {
            throw new IllegalArgumentException("Reservation ID cannot be null");
        }
        if (message.getUserId() == null || message.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }

    private void handleError(String eventType, ReservationNotificationMessage message,
                             Map<String, ?> headers, Exception e) {
        String correlationId = headers != null ? (String) headers.get("X-Correlation-ID") : "N/A";
        logger.error("Error processing {} notification. ReservationId: {}, CorrelationId: {}, Error: {}",
                eventType, message != null ? message.getReservationId() : "N/A", correlationId, e.getMessage(), e);
    }

    private void acknowledgeMessage(Channel channel, Map<String, ?> headers) {
        try {
            Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
            if (deliveryTag != null) {
                channel.basicAck(deliveryTag, false);
                logger.debug("Message acknowledged. DeliveryTag: {}", deliveryTag);
            }
        } catch (IOException e) {
            logger.error("Error acknowledging message", e);
        }
    }

    private void rejectMessage(Channel channel, Map<String, ?> headers) {
        try {
            Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
            if (deliveryTag != null) {
                channel.basicNack(deliveryTag, false, true);
                logger.warn("Message rejected and requeued. DeliveryTag: {}", deliveryTag);
            }
        } catch (IOException e) {
            logger.error("Error rejecting message", e);
        }
    }
}

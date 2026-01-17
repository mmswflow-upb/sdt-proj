package com.example.reservationservice.service;
import com.example.reservationservice.dto.ReservationNotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationPublisher {
    private static final Logger logger = LoggerFactory.getLogger(NotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.reservations}")
    private String reservationsExchange;

    @Value("${rabbitmq.message.ttl:60000}")
    private String messageTTL;

    public NotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishReservationCreated(Long reservationId, String userId, String roomId,
                                          LocalDateTime startDateTime, LocalDateTime endDateTime, String status) {
        ReservationNotificationMessage message = new ReservationNotificationMessage(
                "RESERVATION_CREATED",
                reservationId,
                userId,
                roomId,
                startDateTime,
                endDateTime,
                status,
                LocalDateTime.now()
        );
        publishMessage(message, "reservation.created");
    }

    public void publishReservationCancelled(Long reservationId, String userId, String roomId, String status) {
        ReservationNotificationMessage message = new ReservationNotificationMessage(
                "RESERVATION_CANCELLED",
                reservationId,
                userId,
                roomId,
                null,
                null,
                status,
                LocalDateTime.now()
        );
        publishMessage(message, "reservation.cancelled");
    }

    public void publishReservationRevoked(Long reservationId, String userId, String roomId, String status) {
        ReservationNotificationMessage message = new ReservationNotificationMessage(
                "RESERVATION_REVOKED",
                reservationId,
                userId,
                roomId,
                null,
                null,
                status,
                LocalDateTime.now()
        );
        publishMessage(message, "reservation.revoked");
    }

    public void publishReservationApproved(Long reservationId, String userId, String roomId, String status) {
        ReservationNotificationMessage message = new ReservationNotificationMessage(
                "RESERVATION_APPROVED",
                reservationId,
                userId,
                roomId,
                null,
                null,
                status,
                LocalDateTime.now()
        );
        publishMessage(message, "reservation.approved");
    }

    private void publishMessage(ReservationNotificationMessage message, String routingKey) {
        String correlationId = UUID.randomUUID().toString();
        try {
            rabbitTemplate.convertAndSend(reservationsExchange, routingKey, message, msg -> {
                msg.getMessageProperties().setExpiration(messageTTL);
                msg.getMessageProperties().setHeader("X-Correlation-ID", correlationId);
                msg.getMessageProperties().setHeader("X-Message-Version", message.getMessageVersion());
                return msg;
            });
            logger.info("Message published. CorrelationId: {}, EventType: {}, RoutingKey: {}",
                    correlationId, message.getEventType(), routingKey);
        } catch (Exception e) {
            logger.error("Error publishing message. CorrelationId: {}, EventType: {}, RoutingKey: {}",
                    correlationId, message.getEventType(), routingKey, e);
            throw e;
        }
    }
}

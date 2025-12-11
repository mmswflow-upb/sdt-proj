package com.example.notificationservice.listener;
import com.example.notificationservice.dto.ReservationNotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
@Component
public class ReservationNotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(ReservationNotificationListener.class);
    @RabbitListener(queues = "${rabbitmq.queue.reservation-created}")
    public void handleReservationCreated(ReservationNotificationMessage message) {
        logger.info("=== RESERVATION CREATED NOTIFICATION ===");
        logger.info("Event Type: {}", message.getEventType());
        logger.info("Reservation ID: {}", message.getReservationId());
        logger.info("User ID: {}", message.getUserId());
        logger.info("Room ID: {}", message.getRoomId());
        logger.info("Time: {} to {}", message.getStartDateTime(), message.getEndDateTime());
        logger.info("Status: {}", message.getStatus());
        logger.info("Timestamp: {}", message.getTimestamp());
        logger.info("========================================");
    }
    @RabbitListener(queues = "${rabbitmq.queue.reservation-cancelled}")
    public void handleReservationCancelled(ReservationNotificationMessage message) {
        logger.info("=== RESERVATION CANCELLED NOTIFICATION ===");
        logger.info("Event Type: {}", message.getEventType());
        logger.info("Reservation ID: {}", message.getReservationId());
        logger.info("User ID: {}", message.getUserId());
        logger.info("Room ID: {}", message.getRoomId());
        logger.info("Status: {}", message.getStatus());
        logger.info("Timestamp: {}", message.getTimestamp());
        logger.info("==========================================");
    }
    @RabbitListener(queues = "${rabbitmq.queue.reservation-revoked}")
    public void handleReservationRevoked(ReservationNotificationMessage message) {
        logger.info("=== RESERVATION REVOKED NOTIFICATION ===");
        logger.info("Event Type: {}", message.getEventType());
        logger.info("Reservation ID: {}", message.getReservationId());
        logger.info("User ID: {}", message.getUserId());
        logger.info("Room ID: {}", message.getRoomId());
        logger.info("Status: {}", message.getStatus());
        logger.info("Timestamp: {}", message.getTimestamp());
        logger.info("========================================");
    }
    @RabbitListener(queues = "${rabbitmq.queue.reservation-approved}")
    public void handleReservationApproved(ReservationNotificationMessage message) {
        logger.info("=== RESERVATION APPROVED NOTIFICATION ===");
        logger.info("Event Type: {}", message.getEventType());
        logger.info("Reservation ID: {}", message.getReservationId());
        logger.info("User ID: {}", message.getUserId());
        logger.info("Room ID: {}", message.getRoomId());
        logger.info("Status: {}", message.getStatus());
        logger.info("Timestamp: {}", message.getTimestamp());
        logger.info("=========================================");
    }
}

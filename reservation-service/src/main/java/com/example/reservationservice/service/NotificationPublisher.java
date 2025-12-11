package com.example.reservationservice.service;
import com.example.reservationservice.dto.ReservationNotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
@Service
public class NotificationPublisher {
    private final RabbitTemplate rabbitTemplate;
    @Value("${rabbitmq.exchange.reservations}")
    private String reservationsExchange;
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
        rabbitTemplate.convertAndSend(reservationsExchange, "reservation.created", message);
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
        rabbitTemplate.convertAndSend(reservationsExchange, "reservation.cancelled", message);
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
        rabbitTemplate.convertAndSend(reservationsExchange, "reservation.revoked", message);
    }
}

package com.example.reservationservice.entity;

/**
 * Enumeration of possible reservation states. PENDING when initially created, APPROVED when an admin
 * accepts the reservation, REVOKED when invalidated by an admin action (e.g. room deletion),
 * and CANCELLED when a student cancels their own reservation.
 */
public enum ReservationStatus {
    PENDING,
    APPROVED,
    REVOKED,
    CANCELLED
}
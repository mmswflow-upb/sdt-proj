package com.example.reservationservice.entity;

/**
 * Enumeration of possible reservation states. PENDING when initially created, APPROVED when an admin
 * accepts the reservation, and REVOKED when invalidated by an admin action (e.g. room deletion).
 */
public enum ReservationStatus {
    PENDING,
    APPROVED,
    REVOKED
}
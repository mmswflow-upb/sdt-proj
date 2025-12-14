# Message Queue Architecture

This document explains how RabbitMQ messaging works in the Campus Reservation System.

## Overview

The system uses RabbitMQ for asynchronous communication between the Reservation Service and Notification Service. When reservation events happen, messages are published to RabbitMQ and consumed by the notification service independently.

## Architecture Pattern

The implementation uses a **publish-subscribe pattern** with topic exchange and routing keys.

```
Reservation Service (Publisher)
    |
    | publishes message
    v
Topic Exchange (reservations.exchange)
    |
    | routes by routing key
    v
Queues (reservation.created, reservation.cancelled, etc.)
    |
    | consumes messages
    v
Notification Service (Consumer)
```

## Components

### Publisher - Reservation Service

The reservation service publishes messages when these events occur:
- Reservation created
- Reservation approved
- Reservation cancelled
- Reservation revoked

**How it works:**

1. Uses `RabbitTemplate` to send messages
2. Converts Java objects to JSON automatically
3. Sends to topic exchange with specific routing key
4. Does not wait for response

Example from `NotificationPublisher.java`:
```java
rabbitTemplate.convertAndSend(
    "reservations.exchange",     // exchange name
    "reservation.created",        // routing key
    message                       // message object
);
```

### Exchange - Topic Exchange

The topic exchange routes messages to queues based on routing keys.

**Configuration:**
- Exchange name: `reservations.exchange`
- Type: Topic
- Durable: Yes

**Routing keys used:**
- `reservation.created`
- `reservation.approved`
- `reservation.cancelled`
- `reservation.revoked`

### Queues

Four separate queues store messages until consumed:
- `reservation.created`
- `reservation.approved`
- `reservation.cancelled`
- `reservation.revoked`

Each queue is bound to the topic exchange with its matching routing key.

### Consumer - Notification Service

The notification service listens to all four queues using `@RabbitListener`.

**How it works:**

1. Spring AMQP listens to queues
2. Converts JSON messages back to Java objects
3. Calls listener methods when messages arrive
4. Processes messages independently

Example from `ReservationNotificationListener.java`:
```java
@RabbitListener(queues = "${rabbitmq.queue.reservation-created}")
public void handleReservationCreated(ReservationNotificationMessage message) {
    logger.info("Reservation created: {}", message.getReservationId());
}
```

## Message Format

Messages are JSON objects with this structure:

```json
{
  "eventType": "RESERVATION_CREATED",
  "reservationId": 123,
  "userId": "user456",
  "roomId": "room789",
  "startDateTime": "2025-12-15T10:00:00",
  "endDateTime": "2025-12-15T12:00:00",
  "status": "PENDING",
  "timestamp": "2025-12-14T09:30:00"
}
```

## One-Way vs Two-Way Communication

### One-Way Communication

RabbitMQ messaging is **one-way** by default. The publisher sends a message and immediately continues without waiting for a response.

**Characteristics:**
- Publisher does not block
- Consumer processes messages at its own pace
- No direct response to publisher
- Fire and forget

**Example flow:**
1. User creates reservation
2. Reservation service saves to database
3. Reservation service publishes message to RabbitMQ
4. Reservation service returns response to user
5. Later, notification service processes the message

### Two-Way Communication

RabbitMQ can support request-reply patterns using:
- Reply-to queues
- Correlation IDs
- RPC over AMQP

**How it works:**
1. Publisher sends message with reply-to queue and correlation ID
2. Publisher waits on reply-to queue
3. Consumer processes message
4. Consumer sends response to reply-to queue with same correlation ID
5. Publisher receives response

**Why we don't use it:**

Notifications don't need immediate responses. Using one-way messaging:
- Keeps reservation operations fast
- Prevents notification failures from blocking reservations
- Allows notification service to scale independently
- Supports offline processing and retries

## HTTP vs Message Queue Communication

### HTTP (Synchronous)

Used for operations requiring immediate responses:

**Reservation Service -> Scheduling Service:**
```
Request: Check if room is available
Wait for response
Response: Room is available/unavailable
Continue based on response
```

Characteristics:
- Blocking call
- Immediate response required
- Direct request-response
- Timeout if service unavailable

### Message Queue (Asynchronous)

Used for notifications and events:

**Reservation Service -> Notification Service:**
```
Event: Reservation created
Publish to queue
Continue immediately
(Later) Notification service processes event
```

Characteristics:
- Non-blocking
- No response expected
- Decoupled services
- Resilient to service downtime

## Benefits of This Architecture

**Decoupling:**
- Reservation service doesn't know about notification service
- Services can be deployed independently
- Changes to notification logic don't affect reservations

**Resilience:**
- Messages persist in queues if notification service is down
- Notification service processes messages when it restarts
- No data loss during failures

**Performance:**
- Reservation operations complete faster
- No waiting for notification processing
- Can handle traffic spikes

**Scalability:**
- Can run multiple notification service instances
- Messages distributed among consumers
- Each service scales independently

## Configuration

### Reservation Service

Environment variables:
- `RABBITMQ_HOST` - RabbitMQ server hostname
- `RABBITMQ_PORT` - RabbitMQ server port
- `RABBITMQ_USERNAME` - Authentication username
- `RABBITMQ_PASSWORD` - Authentication password
- `RABBITMQ_EXCHANGE_RESERVATIONS` - Exchange name

### Notification Service

Environment variables:
- Same connection settings as reservation service
- `RABBITMQ_QUEUE_RESERVATION_CREATED` - Queue name
- `RABBITMQ_QUEUE_RESERVATION_CANCELLED` - Queue name
- `RABBITMQ_QUEUE_RESERVATION_REVOKED` - Queue name
- `RABBITMQ_QUEUE_RESERVATION_APPROVED` - Queue name

## Monitoring

Access RabbitMQ Management Console:
- URL: http://localhost:15672
- Username: guest
- Password: guest

You can view:
- Messages in queues
- Message rates
- Queue bindings
- Exchange routing
- Connection status

## When Messages Are Published

**Reservation Created:**
- User successfully creates a reservation
- Reservation saved to database
- Message published with reservation details

**Reservation Approved:**
- Admin approves a pending reservation
- Status updated to APPROVED
- Message published

**Reservation Cancelled:**
- User cancels their own reservation
- Status updated to CANCELLED
- Message published

**Reservation Revoked:**
- Admin revokes an approved reservation
- Status updated to REVOKED
- Message published

## Error Handling

**If message publishing fails:**
- Reservation still succeeds
- Error logged but not thrown
- User won't receive notification

**If message consuming fails:**
- Message requeued automatically
- Notification service retries
- Dead letter queue can be configured for permanent failures

**If notification service is down:**
- Messages accumulate in queues
- Processed when service restarts
- No messages lost

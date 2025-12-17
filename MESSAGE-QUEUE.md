# Message Queue Architecture

We use RabbitMQ for asynchronous communication between Reservation Service and Notification Service.

## Architecture Pattern

We implement a publish-subscribe pattern with topic exchange and routing keys.

## Components

### Publisher

Reservation Service publishes messages for these events:
- Reservation created
- Reservation approved
- Reservation cancelled
- Reservation revoked

We use RabbitTemplate to send messages to the topic exchange with specific routing keys. The service converts Java objects to JSON automatically and does not wait for responses.

### Exchange

Topic exchange routes messages to queues based on routing keys.

Configuration:
- Exchange name: reservations.exchange
- Type: Topic
- Durable: Yes

Routing keys:
- reservation.created
- reservation.approved
- reservation.cancelled
- reservation.revoked

### Queues

Four queues store messages until consumed. Each queue is bound to the topic exchange with its matching routing key.

### Consumer

Notification Service listens to all four queues using @RabbitListener. Spring AMQP converts JSON messages back to Java objects and calls listener methods when messages arrive.

## Message Format

Messages contain:
- eventType
- reservationId
- userId
- roomId
- startDateTime
- endDateTime
- status
- timestamp

## Communication Patterns

### One-Way Communication

RabbitMQ messaging is one-way by default. The publisher sends a message and immediately continues without waiting for a response.

Flow:
1. User creates reservation
2. Reservation service saves to database
3. Reservation service publishes message to RabbitMQ
4. Reservation service returns response to user
5. Notification service processes the message later

Benefits:
- Publisher does not block
- Consumer processes at its own pace
- Fire and forget
- Keeps reservation operations fast
- Prevents notification failures from blocking reservations
- Allows notification service to scale independently
- Supports offline processing and retries

## HTTP vs Message Queue

### HTTP

We use HTTP for operations requiring immediate responses like checking room availability with Scheduling Service.

Characteristics:
- Blocking call
- Immediate response required
- Direct request-response
- Timeout if service unavailable

### Message Queue

We use message queues for notifications and events.

Characteristics:
- Non-blocking
- No response expected
- Decoupled services
- Resilient to service downtime

## Benefits

Decoupling:
- Reservation service does not know about notification service
- Services can be deployed independently
- Changes to notification logic do not affect reservations

Resilience:
- Messages persist in queues if notification service is down
- Notification service processes messages when it restarts
- No data loss during failures

Performance:
- Reservation operations complete faster
- No waiting for notification processing
- Can handle traffic spikes

Scalability:
- Can run multiple notification service instances
- Messages distributed among consumers
- Each service scales independently

## Configuration

### Reservation Service

Environment variables:
- RABBITMQ_HOST
- RABBITMQ_PORT
- RABBITMQ_USERNAME
- RABBITMQ_PASSWORD
- RABBITMQ_EXCHANGE_RESERVATIONS

### Notification Service

Environment variables:
- Same connection settings as reservation service
- RABBITMQ_QUEUE_RESERVATION_CREATED
- RABBITMQ_QUEUE_RESERVATION_CANCELLED
- RABBITMQ_QUEUE_RESERVATION_REVOKED
- RABBITMQ_QUEUE_RESERVATION_APPROVED

## Monitoring

Access RabbitMQ Management Console at http://localhost:15672 with guest/guest credentials.

View:
- Messages in queues
- Message rates
- Queue bindings
- Exchange routing
- Connection status

## Event Publishing

Reservation Created:
- User creates a reservation
- Reservation saved to database
- Message published with reservation details

Reservation Approved:
- Admin approves a pending reservation
- Status updated to APPROVED
- Message published

Reservation Cancelled:
- User cancels their reservation
- Status updated to CANCELLED
- Message published

Reservation Revoked:
- Admin revokes an approved reservation
- Status updated to REVOKED
- Message published

## Error Handling

If message publishing fails:
- Reservation still succeeds
- Error logged but not thrown
- User will not receive notification

If message consuming fails:
- Message requeued automatically
- Notification service retries
- Dead letter queue can be configured for permanent failures

If notification service is down:
- Messages accumulate in queues
- Processed when service restarts
- No messages lost

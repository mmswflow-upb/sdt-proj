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
- messageVersion - Version of the message format (1.0)
- eventType - Type of reservation event
- reservationId - ID of the reservation
- userId - ID of the user
- roomId - ID of the room
- startDateTime - Event start time
- endDateTime - Event end time
- status - Reservation status
- timestamp - Message creation time

Message headers:
- X-Correlation-ID - Unique ID for tracking message through the system
- X-Message-Version - Message format version for compatibility

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
- RABBITMQ_HOST - RabbitMQ server hostname
- RABBITMQ_PORT - RabbitMQ server port
- RABBITMQ_USERNAME - RabbitMQ credentials
- RABBITMQ_PASSWORD - RabbitMQ credentials
- RABBITMQ_EXCHANGE_RESERVATIONS - Topic exchange name
- RABBITMQ_MESSAGE_TTL - Message time to live in milliseconds (default: 60000)

Publisher confirms enabled:
- spring.rabbitmq.publisher-confirms: true
- spring.rabbitmq.publisher-returns: true

### Notification Service

Environment variables:
- RABBITMQ_HOST - Same as reservation service
- RABBITMQ_PORT - Same as reservation service
- RABBITMQ_USERNAME - Same as reservation service
- RABBITMQ_PASSWORD - Same as reservation service
- RABBITMQ_QUEUE_RESERVATION_CREATED - Queue name for created events
- RABBITMQ_QUEUE_RESERVATION_CANCELLED - Queue name for cancelled events
- RABBITMQ_QUEUE_RESERVATION_REVOKED - Queue name for revoked events
- RABBITMQ_QUEUE_RESERVATION_APPROVED - Queue name for approved events
- RABBITMQ_EXCHANGE_RESERVATIONS - Topic exchange name
- RABBITMQ_EXCHANGE_DLX - Dead letter exchange name
- RABBITMQ_QUEUE_DLQ - Dead letter queue name
- RABBITMQ_MESSAGE_TTL - Message time to live in milliseconds

Manual acknowledgment enabled:
- spring.rabbitmq.listener.simple.acknowledge-mode: MANUAL

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
- Error logged with correlation ID
- Publisher confirms track delivery status

If message consuming fails:
- Message validation checks for required fields
- On validation failure: message rejected and sent to dead letter queue
- Detailed error logging includes correlation ID and reservation ID
- Dead letter queue handler logs all failures for manual review

If notification service is down:
- Messages accumulate in queues
- Processed when service restarts
- No messages lost

## Reliability Features

### Publisher Confirms
- Publisher confirms enabled in reservation service
- Confirms track successful message delivery to broker
- Failed publishes logged with correlation ID
- Return callbacks handle unroutable messages

### Message TTL (Time To Live)
- Default: 60 seconds (configurable via RABBITMQ_MESSAGE_TTL)
- Messages automatically expire and move to dead letter queue if not consumed
- Prevents stale message accumulation
- Configurable per deployment

### Manual Message Acknowledgment
- Notification service uses manual acknowledgment mode
- Messages acknowledged only after successful processing
- Failed messages rejected and requeued or sent to DLQ
- Prevents message loss during processing failures

### Dead Letter Queue
- Exchange: reservations.dlx
- Queue: reservation-dead-letter-queue
- Captures messages that:
  - Fail validation
  - Are redelivered too many times
  - Expire due to TTL
- Dedicated listener logs all DLQ messages for investigation
- Tracks redelivery count and original routing information

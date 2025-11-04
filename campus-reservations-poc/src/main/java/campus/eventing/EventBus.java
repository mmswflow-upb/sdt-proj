package campus.eventing;
public interface EventBus {
    void publish(DomainEvent event);
    void subscribe(DomainEventType type, EventListener listener);
}

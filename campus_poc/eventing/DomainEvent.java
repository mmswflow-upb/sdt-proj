package campus_poc.eventing;

public class DomainEvent {
    private final DomainEventType type;
    private final String aggregateId;
    private final Object payload;

    public DomainEvent(DomainEventType type, String aggregateId, Object payload) {
        this.type = type;
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    public DomainEventType getType() { return type; }
    public String getAggregateId() { return aggregateId; }
    public Object getPayload() { return payload; }

    @Override public String toString() {
        return "DomainEvent{" + type + ", id=" + aggregateId + "}";
    }
}

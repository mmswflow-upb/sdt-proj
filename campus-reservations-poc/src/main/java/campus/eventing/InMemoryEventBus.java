package campus.eventing;

import java.util.*;

public class InMemoryEventBus implements EventBus {
    private static final InMemoryEventBus INSTANCE = new InMemoryEventBus();
    public static InMemoryEventBus getInstance() { return INSTANCE; }

    private final Map<DomainEventType, List<EventListener>> routes = new EnumMap<>(DomainEventType.class);
    private InMemoryEventBus() {}

    @Override
    public void publish(DomainEvent event) {
        List<EventListener> ls = routes.getOrDefault(event.getType(), List.of());
        for (EventListener l : ls) l.on(event);
    }

    @Override
    public void subscribe(DomainEventType type, EventListener listener) {
        routes.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }
}

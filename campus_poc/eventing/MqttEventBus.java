package campus_poc.eventing;

import java.util.*;

public class MqttEventBus implements EventBus {
    private static volatile MqttEventBus INSTANCE;
    public static  MqttEventBus getInstance(MqttClient client) {
        if (INSTANCE == null) {
                    INSTANCE = new MqttEventBus(client);
        }
        return INSTANCE;
    }

    private final MqttClient client;
    private final Map<DomainEventType, List<EventListener>> local = new EnumMap<>(DomainEventType.class);

    private MqttEventBus(MqttClient client) { this.client = client; }

    @Override
    public void publish(DomainEvent event) {
        client.publish("campus/" + event.getType().name().toLowerCase(), event.toString());
        for (EventListener l : local.getOrDefault(event.getType(), List.of())) l.on(event);
    }

    @Override
    public void subscribe(DomainEventType type, EventListener listener) {
        client.subscribe("campus/" + type.name().toLowerCase(), listener);
        local.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }
}

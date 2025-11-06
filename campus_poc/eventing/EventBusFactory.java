package campus_poc.eventing;


public final class EventBusFactory {
    private EventBusFactory() {}

    public static EventBus create(EventBusKind kind, MqttClient mqttClient) {
        switch (kind) {
            case IN_MEMORY -> {
                return InMemoryEventBus.getInstance();
            }
            case MQTT -> {
                MqttClient client = mqttClient != null ? mqttClient : new MqttClient();
                return MqttEventBus.getInstance(client);
            }
            default -> throw new IllegalArgumentException("Unsupported EventBus kind: " + kind);
        }
    }
}

package campus_poc.eventing;
public class MqttClient {
    public void publish(String topic, String payload) {
        System.out.println("[MQTT] publish topic=" + topic + " payload=" + payload);
    }
    public void subscribe(String topic, Object handler) {
        System.out.println("[MQTT] subscribe topic=" + topic);
    }
}

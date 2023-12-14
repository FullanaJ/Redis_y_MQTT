import org.eclipse.paho.client.mqttv3.*;
import redis.clients.jedis.Jedis;

public class Radar {

    private static final int SPEED_LIMIT = 80;

    public static void main(String[] args) {
        String broker = "tcp://mqtt.eclipse.org:1883"; // Cambiar al broker MQTT que uses
        String clientId = "Radar";
        Jedis jedis = new Jedis("localhost");

        try {
            MqttClient client = new MqttClient(broker, clientId);
            client.connect();

            client.subscribe("car/speed/#", (topic, message) -> {
                String matricula = topic.substring(topic.lastIndexOf('/') + 1);
                String mensaje = new String(message.getPayload());
                int velocidad;
                String[] partes = mensaje.split(": "); // Dividir el mensaje en partes usando ": "
                // Verificar si hay al menos dos partes y extraer la velocidad si es así
                if (partes.length >= 2) {
                    String velocidadString = partes[1].split(" ")[0]; // Extraer la parte numérica antes del espacio "km/h"
                    velocidad = Integer.parseInt(velocidadString);
                } else {
                    velocidad = 0;
                }
                if (velocidad > SPEED_LIMIT) {
                    // Crear entrada en Redis para exceso de velocidad
                    String key = "EXCESO:" + SPEED_LIMIT + ":" + matricula;
                    jedis.set(key, String.valueOf(velocidad));
                } else {
                    // Añadir a grupo "VEHICULOS" en Redis
                    jedis.sadd("VEHICULOS", matricula);
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }
}

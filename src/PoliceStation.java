import org.eclipse.paho.client.mqttv3.*;
import redis.clients.jedis.Jedis;

import java.util.Set;

public class PoliceStation {

    private static final double LIMIT_10_PERCENT = 0.10;
    private static final double LIMIT_20_PERCENT = 0.20;
    private static final double LIMIT_30_PERCENT = 0.30;

    public static void main(String[] args) {
        String broker = "tcp://mqtt.eclipse.org:1883"; // Cambiar al broker MQTT que uses
        String clientId = "PoliceStation";
        Jedis jedis = new Jedis("localhost"); // Cambiar por la dirección del servidor Redis

        try {
            MqttClient client = new MqttClient(broker, clientId);
            client.connect();

            while (true) {
                Set<String> excessKeys = jedis.keys("EXCESO:*");
                int totalVehicles = jedis.smembers("VEHICULOS").size();
                int reportedVehicles = jedis.smembers("VEHICULOSDENUNCIADOS").size();

                // Mostrar estadísticas
                System.out.println("Número total de vehículos: " + totalVehicles);
                double percentageReported = (double) reportedVehicles / totalVehicles * 100;
                System.out.println("Porcentaje de vehículos multados: " + percentageReported + "%");

                // Procesar multas
                for (String key : excessKeys) {
                    String plate = key.split(":")[2]; // Extraer la matrícula desde la clave
                    String speedString = jedis.get(key);
                    int speed = Integer.parseInt(speedString);

                    // Calcular el importe de la multa según la velocidad
                    int fineAmount = calculateFineAmount(speed);

                    // Enviar multa por MQTT
                    String topic = "multas/" + plate;
                    String message = "Multa de " + fineAmount + " € por exceso de velocidad";
                    client.publish(topic, message.getBytes(), 0, false);

                    // Borrar la clave de Redis correspondiente
                    jedis.del(key);

                    // Añadir la matrícula al grupo "VEHICULOSDENUNCIADOS" en Redis
                    jedis.sadd("VEHICULOSDENUNCIADOS", plate);
                }

                // Esperar aproximadamente un segundo
                Thread.sleep(1000);
            }
        } catch (MqttException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }

    private static int calculateFineAmount(int speed) {
        int speedLimit = 80; // Velocidad límite
        double percentage = ((double) speed - speedLimit) / speedLimit;

        if (percentage <= LIMIT_10_PERCENT) {
            return 100;
        } else if (percentage <= LIMIT_20_PERCENT) {
            return 200;
        } else if (percentage <= LIMIT_30_PERCENT) {
            return 500;
        } else {
            return 0; // No se aplica multa si es mayor al 30% de la velocidad límite
        }
    }
}

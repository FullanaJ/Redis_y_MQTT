import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.util.Random;

public class CarSimulator {
    public static void main(String[] args) {
        String broker = "tcp://mqtt.eclipse.org:1883"; // Cambiar al broker MQTT que uses
        String clientId = "CarSimulator";
        try {
            MqttClient client = new MqttClient(broker, clientId);
            client.connect();
            Random random = new Random();

            while (true) {
                // Generar velocidad aleatoria entre 60 y 140
                int velocidad = random.nextInt(81) + 60;

                // Generar matrícula aleatoria (4 dígitos y 3 letras)
                String matricula = generateRandomPlate();

                // Enviar mensaje MQTT con la velocidad y matrícula
                String topic = "car/speed/" + matricula;
                String message = "Velocidad: " + velocidad + " km/h";
                client.publish(topic, message.getBytes(), 0, false);

                // Simular espera de 1 segundo
                Thread.sleep(1000);
            }

        } catch (MqttPersistenceException|InterruptedException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateRandomPlate() {
        // Generar una matrícula aleatoria (4 dígitos y 3 letras)
        Random random = new Random();
        StringBuilder plate = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            plate.append(random.nextInt(10)); // Añadir dígitos aleatorios
        }

        for (int i = 0; i < 3; i++) {
            char letter = (char) (random.nextInt(26) + 'A'); // Añadir letras aleatorias
            plate.append(letter);
        }

        return plate.toString();
    }
}


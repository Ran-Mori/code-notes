package main;

import java.io.IOException;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;

public class ProducerExample {

    public static void main(String[] args) throws ClientException, IOException {
        String endpoint = "localhost:8081";
        String topic = "TestTopic";
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder().setEndpoints(endpoint).build();
        Producer producer = provider.newProducerBuilder()
                .setTopics(topic)
                .setClientConfiguration(configuration)
                .build();
        Message message = provider.newMessageBuilder()
                .setTopic(topic)
                .setKeys("messageKey")
                .setTag("messageTag")
                .setBody("messageBody".getBytes())
                .build();
        try {
            SendReceipt sendReceipt = producer.send(message);
            System.out.println("Send message successfully, messageId=" + sendReceipt.getMessageId());
        } catch (ClientException e) {
            System.out.println("Failed to send message" + e);
        }
        // producer.close();
    }
}

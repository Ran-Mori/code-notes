package main;

import java.io.IOException;
import java.util.Collections;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;

public class PushConsumerExample {
    private PushConsumerExample() {
    }

    public static void main(String[] args) throws ClientException, IOException, InterruptedException {
        final ClientServiceProvider provider = ClientServiceProvider.loadService();
        String endpoints = "localhost:8081";
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                .setEndpoints(endpoints)
                .build();
        String tag = "*";
        FilterExpression filterExpression = new FilterExpression(tag, FilterExpressionType.TAG);
        String consumerGroup = "YourConsumerGroup";
        String topic = "TestTopic";
        PushConsumer pushConsumer = provider.newPushConsumerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setConsumerGroup(consumerGroup)
                .setSubscriptionExpressions(Collections.singletonMap(topic, filterExpression))
                .setMessageListener(messageView -> {
                    System.out.println("Consume message successfully, messageId=" + messageView.getMessageId());
                    return ConsumeResult.SUCCESS;
                })
                .build();
        Thread.sleep(Long.MAX_VALUE);
        // pushConsumer.close();
    }
}

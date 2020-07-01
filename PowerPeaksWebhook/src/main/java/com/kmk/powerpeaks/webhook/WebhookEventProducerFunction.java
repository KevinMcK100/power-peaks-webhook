package com.kmk.powerpeaks.webhook;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kmk.powerpeaks.webhook.model.WebhookEventRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WebhookEventProducerFunction implements RequestStreamHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

        System.out.println("Executing producer!");
        JsonNode jsonNode = objectMapper.readTree(inputStream);
        String body = jsonNode.get("body").asText();
        System.out.println("Before mapping: " + body);
        System.out.println("Object mapper: " + objectMapper.toString());
        WebhookEventRequest webhookEventRequest = objectMapper.readValue(body, WebhookEventRequest.class);
        System.out.println("After mapping: " + webhookEventRequest.toString());

        String accountId = System.getenv("ACCOUNT_ID");
        System.out.println("Account ID: " + accountId);
        String sqsQueueArn = System.getenv("SQS_QUEUE_ARN");
        System.out.println("SQS Queue ARN: " + sqsQueueArn);

        String topicArn = System.getenv("SNS_TOPIC_ARN");
        System.out.println("Topic ARN: " + topicArn);
        final PublishRequest publishRequest = new PublishRequest(topicArn, objectMapper.writeValueAsString(webhookEventRequest));
        AmazonSNS snsClient = AmazonSNSClient.builder().withRegion(Regions.EU_WEST_1).build();
        snsClient.publish(publishRequest);

        String sqsQueue = System.getenv("SQS_QUEUE");
        System.out.println("SQS Queue: " + sqsQueue);
        AmazonSQS sqs = AmazonSQSClient.builder().withRegion(Regions.EU_WEST_1).build();
        SendMessageRequest messageRequest = new SendMessageRequest()
                .withQueueUrl(sqsQueue)
                .withMessageBody(objectMapper.writeValueAsString(webhookEventRequest));
        sqs.sendMessage(messageRequest);

    }
}

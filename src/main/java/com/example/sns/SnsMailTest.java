package com.example.sns;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.time.LocalDateTime;

/**
 * Email sending test via AWS SNS (Publish Only)
 *
 * Usage:
 * Set the following environment variables before executing:
 * SNS_TOPIC_ARN : The ARN of the SNS topic (Required)
 * AWS_REGION    : AWS Region (Default: ap-northeast-1)
 */
public class SnsMailTest {

    public static void main(String[] args) {
        // ── 1. Load configurations from environment variables ─────────────
        String topicArn = System.getenv("SNS_TOPIC_ARN");
        String regionStr = System.getenv().getOrDefault("AWS_REGION", "ap-northeast-1");

        if (topicArn == null || topicArn.isBlank()) {
            System.err.println("[ERROR] Environment variable SNS_TOPIC_ARN is not set.");
            System.exit(1);
        }

        System.out.println("=== SNS Publish Test ===");
        System.out.println("Region    : " + regionStr);
        System.out.println("Topic ARN : " + topicArn);

        // ── 2. Create SNS Client ──────────────────────────────────────────
        SnsClient snsClient = SnsClient.builder()
                .region(Region.of(regionStr))
                .build();

        // ── 3. Publish message to the Topic ───────────────────────────────
        System.out.println("\nPublishing message to SNS Topic...");

        String messageBody = String.format(
                "[SNS Test] Email sent from AWS Fargate\n\n" +
                "This is a test message published to the SNS Topic from a Java application.\n\n" +
                "Timestamp : %s\n" +
                "Region    : %s\n" +
                "Topic ARN : %s",
                LocalDateTime.now(),
                regionStr,
                topicArn
        );

        try {
            PublishRequest request = PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject("[Test] Fargate SNS Notification")
                    .message(messageBody)
                    .build();

            PublishResponse result = snsClient.publish(request);

            System.out.println("[SUCCESS] Message published successfully!");
            System.out.println("          MessageId: " + result.messageId());
            System.out.println("          Please check the subscribed email inboxes.");

        } catch (SnsException e) {
            System.err.println("[ERROR] Failed to publish message.");
            System.err.println("        Error Details: " + e.awsErrorDetails().errorMessage());
        } finally {
            snsClient.close();
        }
    }
}
package com.example.ses;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SendEmailResponse;
import software.amazon.awssdk.services.ses.model.SesException;

/**
 * Email sending test via AWS SES
 *
 * Usage:
 * Set the following environment variables before executing:
 * SES_SENDER_EMAIL    : The verified sender email address in AWS SES (Required)
 * SES_RECIPIENT_EMAIL : The recipient email address (Required)
 * AWS_REGION          : AWS Region (Default: ap-northeast-1)
 */
public class SesMailTest {

    public static void main(String[] args) {
        // ── Load configurations from environment variables ─────────────
        String senderEmail = System.getenv("SES_SENDER_EMAIL");
        String recipientEmail = System.getenv("SES_RECIPIENT_EMAIL");
        String regionStr = System.getenv().getOrDefault("AWS_REGION", "ap-northeast-1");

        if (senderEmail == null || senderEmail.isBlank()) {
            System.err.println("[ERROR] Environment variable SES_SENDER_EMAIL is not set.");
            System.exit(1);
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            System.err.println("[ERROR] Environment variable SES_RECIPIENT_EMAIL is not set.");
            System.exit(1);
        }

        System.out.println("=== SES Mail Test ===");
        System.out.println("Region          : " + regionStr);
        System.out.println("Sender Email    : " + senderEmail);
        System.out.println("Recipient Email : " + recipientEmail);

        // ── Create SES Client ──────────────────────────────────────────
        Region region = Region.of(regionStr);
        SesClient sesClient = SesClient.builder()
                .region(region)
                .build();

        // ── Publish message via SES ────────────────────────────────────
        System.out.println("\nSending email via AWS SES...");

        try {
            sendEmail(sesClient, senderEmail, recipientEmail);
            System.out.println("[SUCCESS] Email sent successfully!");
            System.out.println("          Please check the recipient's inbox.");
        } catch (SesException e) {
            System.err.println("[ERROR] Failed to send email via SES.");
            System.err.println("        Error Message: " + e.awsErrorDetails().errorMessage());
        } finally {
            sesClient.close();
        }
    }

    /**
     * Helper method to construct and send the email
     */
    private static void sendEmail(SesClient client, String sender, String recipient) throws SesException {
        Destination destination = Destination.builder()
                .toAddresses(recipient)
                .build();

        Content subject = Content.builder()
                .data("[Test] Email from AWS Fargate via SES")
                .build();

        String bodyText = String.format(
                "【SES Test】Fargate Email Sending Test\n\n" +
                "This message was sent from a Java application running on AWS Fargate.\n\n" +
                "Timestamp : %s\n" +
                "Sender    : %s\n" +
                "Recipient : %s",
                java.time.LocalDateTime.now(),
                sender,
                recipient
        );

        Content content = Content.builder()
                .data(bodyText)
                .build();

        Body body = Body.builder()
                .text(content)
                .build();

        Message message = Message.builder()
                .subject(subject)
                .body(body)
                .build();

        SendEmailRequest request = SendEmailRequest.builder()
                .source(sender)
                .destination(destination)
                .message(message)
                .build();

        SendEmailResponse response = client.sendEmail(request);
        System.out.println("          MessageId: " + response.messageId());
    }
}
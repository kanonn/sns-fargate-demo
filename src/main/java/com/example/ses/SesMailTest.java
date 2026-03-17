package com.example.sns;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Send emails via AWS SES v2 (multiple recipients supported).
 *
 * Required environment variables:
 *   SES_FROM     : Verified sender email address (must be verified in SES)
 *   SES_TO       : Comma-separated recipient email addresses (e.g. a@x.com,b@x.com)
 *   AWS_REGION   : AWS region (default: ap-northeast-1)
 *
 * On Fargate, authentication is handled automatically via the IAM task role.
 * No AWS_ACCESS_KEY_ID or AWS_SECRET_ACCESS_KEY needed.
 */
public class SesMailTest {

    public static void main(String[] args) {
        // -- Load configuration from environment variables --
        String from      = System.getenv("SES_FROM");
        String toEnv     = System.getenv("SES_TO");
        String region    = System.getenv().getOrDefault("AWS_REGION", "ap-northeast-1");

        // -- Validate required variables --
        if (from == null || from.isBlank()) {
            System.err.println("[ERROR] Environment variable SES_FROM is not set.");
            System.exit(1);
        }
        if (toEnv == null || toEnv.isBlank()) {
            System.err.println("[ERROR] Environment variable SES_TO is not set.");
            System.exit(1);
        }

        // -- Parse comma-separated recipient list --
        List<String> toAddresses = Arrays.stream(toEnv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        System.out.println("=== SES Mail Test (Multiple Recipients) ===");
        System.out.println("Region     : " + region);
        System.out.println("From       : " + from);
        System.out.println("To (" + toAddresses.size() + ")  : " + toAddresses);

        // -- Build SES v2 client (uses IAM task role on Fargate automatically) --
        SesV2Client sesClient = SesV2Client.builder()
                .region(Region.of(region))
                .build();

        // -- Compose email content --
        String subject = "[TEST] SES Email from Fargate";
        String body = String.format(
                "This email was sent from a Java application running on AWS Fargate via SES.\n\n" +
                "Timestamp : %s\n" +
                "Region    : %s\n" +
                "From      : %s\n" +
                "To        : %s",
                LocalDateTime.now(),
                region,
                from,
                String.join(", ", toAddresses)
        );

        // -- Send email --
        System.out.println("\nSending email...");

        SendEmailResponse response = sesClient.sendEmail(
                SendEmailRequest.builder()
                        .fromEmailAddress(from)
                        .destination(
                                Destination.builder()
                                        .toAddresses(toAddresses)
                                        .build()
                        )
                        .content(
                                EmailContent.builder()
                                        .simple(
                                                Message.builder()
                                                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                                                        .body(Body.builder()
                                                                .text(Content.builder().data(body).charset("UTF-8").build())
                                                                .build())
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()
        );

        System.out.println("[SUCCESS] Email sent!");
        System.out.println("          MessageId : " + response.messageId());

        sesClient.close();
    }
}
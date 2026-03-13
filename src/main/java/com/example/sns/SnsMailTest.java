package com.example.sns;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.Subscription;

import java.util.List;

/**
 * SNS経由でメール送信テスト
 *
 * 使い方:
 *   環境変数を設定して実行
 *     SNS_TOPIC_ARN  : SNSトピックのARN (必須)
 *     SNS_EMAIL      : サブスクリプション登録するメールアドレス (初回のみ必要)
 *     AWS_REGION     : AWSリージョン (デフォルト: ap-northeast-1)
 */
public class SnsMailTest {

    public static void main(String[] args) {
        // ── 環境変数から設定を読み込む ──────────────────────────────
        String topicArn = System.getenv("SNS_TOPIC_ARN");
        String email    = System.getenv("SNS_EMAIL");
        String region   = System.getenv().getOrDefault("AWS_REGION", "ap-northeast-1");

        if (topicArn == null || topicArn.isBlank()) {
            System.err.println("[ERROR] 環境変数 SNS_TOPIC_ARN が設定されていません。");
            System.exit(1);
        }

        System.out.println("=== SNS Mail Test ===");
        System.out.println("Region   : " + region);
        System.out.println("TopicARN : " + topicArn);

        // ── SNSクライアント生成 (Fargate上ではタスクロールで自動認証) ──
        SnsClient snsClient = SnsClient.builder()
                .region(Region.of(region))
                .build();

        // ── メールサブスクリプションが未登録なら登録する ────────────
        if (email != null && !email.isBlank()) {
            boolean alreadySubscribed = isAlreadySubscribed(snsClient, topicArn, email);

            if (!alreadySubscribed) {
                System.out.println("メールアドレスを登録中: " + email);
                SubscribeResponse subscribeResponse = snsClient.subscribe(
                        SubscribeRequest.builder()
                                .topicArn(topicArn)
                                .protocol("email")
                                .endpoint(email)
                                .build()
                );
                System.out.println("[INFO] サブスクリプション登録完了。確認メールを承認してください。");
                System.out.println("       SubscriptionArn: " + subscribeResponse.subscriptionArn());
                System.out.println("       ※ メールを確認して「Confirm subscription」をクリックしてください。");
            } else {
                System.out.println("[INFO] " + email + " はすでにサブスクリプション登録済みです。");
            }
        }

        // ── SNSにメッセージをパブリッシュ ────────────────────────────
        System.out.println("\nSNSにメッセージを送信中...");

        String message = String.format(
                "【SNSテスト】Fargateからのメール送信テスト\n\n" +
                "このメッセージはAWS Fargate上のJavaアプリから送信されました。\n\n" +
                "送信時刻: %s\n" +
                "リージョン: %s\n" +
                "トピックARN: %s",
                java.time.LocalDateTime.now(),
                region,
                topicArn
        );

        PublishResponse publishResponse = snsClient.publish(
                PublishRequest.builder()
                        .topicArn(topicArn)
                        .subject("【テスト】FargateからのSNSメール")
                        .message(message)
                        .build()
        );

        System.out.println("[SUCCESS] メッセージ送信完了！");
        System.out.println("          MessageId: " + publishResponse.messageId());
        System.out.println("          メールが届いているか確認してください。");

        snsClient.close();
    }

    /**
     * 指定メールアドレスがすでにサブスクリプション登録されているか確認
     */
    private static boolean isAlreadySubscribed(SnsClient snsClient, String topicArn, String email) {
        try {
            List<Subscription> subscriptions = snsClient.listSubscriptionsByTopic(
                    ListSubscriptionsByTopicRequest.builder()
                            .topicArn(topicArn)
                            .build()
            ).subscriptions();

            return subscriptions.stream()
                    .anyMatch(sub ->
                            "email".equals(sub.protocol()) &&
                            email.equalsIgnoreCase(sub.endpoint())
                    );
        } catch (Exception e) {
            System.out.println("[WARN] サブスクリプション確認中にエラー: " + e.getMessage());
            return false;
        }
    }
}

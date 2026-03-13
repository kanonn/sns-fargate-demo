# ── ビルドステージ ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
# 依存関係を先にダウンロード（キャッシュ活用）
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── 実行ステージ ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/target/sns-fargate-test-1.0.0.jar app.jar

# Fargate上でコンテナが起動したらすぐ実行して終了
ENTRYPOINT ["java", "-jar", "app.jar"]

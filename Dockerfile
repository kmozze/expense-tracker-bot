FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

COPY . .

RUN ./gradlew jooqCodegen --no-daemon \
    && ./gradlew bootJar --no-daemon \
    && JAR_FILE="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" \
    && cp "$JAR_FILE" app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S expense && adduser -S expense -G expense

COPY --from=builder /workspace/app.jar /app/app.jar

RUN chown expense:expense /app/app.jar

USER expense

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

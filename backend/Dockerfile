FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/data/uploads && \
    chmod 755 /app/data && \
    chmod 755 /app/data/uploads

RUN adduser --system --group appuser

RUN chown -R appuser:appuser /app/data

USER appuser

EXPOSE 8000

ENTRYPOINT ["java", \
    "-Dspring.profiles.active=prod", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dfile.upload-dir=/app/data/uploads", \
    "-Dspring.servlet.multipart.max-file-size=50MB", \
    "-Dspring.servlet.multipart.max-request-size=50MB", \
    "-Dserver.tomcat.max-swallow-size=50MB", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "/app/app.jar"]
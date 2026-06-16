FROM eclipse-temurin:8-jdk-alpine
VOLUME /tmp
COPY target/ocean-server-1.0.0-exec.jar app.jar
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/app.jar", "--spring.profiles.active=prod"]

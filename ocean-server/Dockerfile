FROM eclipse-temurin:11-jre-alpine
VOLUME /tmp
COPY target/ocean-server-1.0.0-exec.jar app.jar
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3", "-jar", "/app.jar", "--spring.profiles.active=prod"]
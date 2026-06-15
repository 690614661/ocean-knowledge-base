FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY target/ocean-server-1.0.0.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=prod"]

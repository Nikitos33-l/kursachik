FROM eclipse-temurin:21-jre-alpine-3.23
WORKDIR /app
COPY target/kursach-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
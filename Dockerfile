FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Sem -alpine: a imagem alpine do Java 17 não existe para Mac com chip Apple (arm64)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/backend2-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082 3000
ENTRYPOINT ["java", "-jar", "app.jar"]

# Fase 1: Compilazione del progetto con Maven usando Java 25
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Fase 2: Esecuzione dell'applicazione con Java 25
FROM eclipse-temurin:25-jre
WORKDIR /app
# Copia il file jar generato dalla fase precedente
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
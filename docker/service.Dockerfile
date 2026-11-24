# Dockerfile genérico para los microservicios Spring Boot (A05).
# Se construye desde la raíz del repo:  docker build -f docker/service.Dockerfile --build-arg MODULE=user-service .

# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-17 AS build
ARG MODULE
WORKDIR /workspace

# Primero solo los pom.xml para aprovechar la caché de dependencias
COPY pom.xml .
COPY common-events/pom.xml common-events/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY question-service/pom.xml question-service/
COPY simulation-service/pom.xml simulation-service/
COPY tracking-service/pom.xml tracking-service/
COPY swing-client/pom.xml swing-client/
RUN mvn -B -q -pl ${MODULE} -am dependency:go-offline -DskipTests || true

COPY common-events/src common-events/src
COPY ${MODULE}/src ${MODULE}/src
RUN mvn -B -q -pl ${MODULE} -am package -DskipTests \
    && cp ${MODULE}/target/${MODULE}-*.jar /workspace/app.jar

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app && apk add --no-cache wget
USER app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

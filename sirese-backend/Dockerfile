FROM maven3.9.6-eclipse-temurin-17 AS build
WORKDIR app
COPY pom.xml .
COPY src .src
RUN mvn clean package -DskipTests

FROM eclipse-temurin17-jre-alpine
WORKDIR app
COPY --from=build apptargetsirese-1.0.0.jar app.jar
 
EXPOSE 8080
 
ENTRYPOINT [java, -jar, app.jar]
 
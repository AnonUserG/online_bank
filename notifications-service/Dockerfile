FROM eclipse-temurin:21-jre-alpine

ENV APP_HOME=/app
WORKDIR ${APP_HOME}

COPY target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java","-jar","/app/app.jar"]

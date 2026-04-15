FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -g 1000 -S app && adduser -u 1000 -S app -G app
WORKDIR /app
ARG JAR_PATH
COPY ${JAR_PATH} app.jar
RUN chown -R app:app /app

USER app
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

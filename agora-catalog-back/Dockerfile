# Imagen del backend: compila el jar con Gradle y lo corre en un JRE 21.
#
#   docker build -t agora-catalog-back .
#
# La configuración entra por variables de entorno (DB_URL, DB_USERNAME, DB_PASSWORD...), igual que
# el .env de .env.example; el .env no entra a la imagen (.dockerignore).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# Capa aparte para las dependencias: un cambio de código no las vuelve a bajar.
RUN ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon -q bootJar && cp build/libs/agora-catalog-back-*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 9092
ENTRYPOINT ["java", "-jar", "app.jar"]

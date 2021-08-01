FROM maven:3.8.1-openjdk-11 AS build

WORKDIR /usr/src/app

COPY ./pom.xml ./
# Resole dependencies
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:resolve

COPY ./src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -D skipTests

FROM openjdk:11-jre-slim AS runtime

RUN apt-get update && \
    apt-get install -y chromium chromium-driver

WORKDIR /usr/src/app

COPY --from=build /usr/src/app/src/main/assets/* ./
COPY --from=build /usr/src/app/target/traq-music-station-jar-with-dependencies.jar ./traq-music-station.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dwebdriver.chrome.driver=/usr/bin/chromedriver", "./traq-music-station.jar"]

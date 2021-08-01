FROM maven:3.8.1-openjdk-11 AS build

WORKDIR /usr/src/app

COPY ./pom.xml ./
# Resole dependencies
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:resolve

COPY ./src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B package -D skipTests

FROM openjdk:11-jre-slim AS runtime

RUN apt-get update && \
    apt-get install -y chromium chromium-driver wget zip

WORKDIR /usr/src/app

COPY --from=build /usr/src/app/src/main/assets/* ./
COPY --from=build /usr/src/app/target/traq-music-station-jar-with-dependencies.jar ./traq-music-station.jar

# add libconnector.so for aarch64
RUN mkdir -p natives/linux-aarch64 && \
    wget -O natives/linux-aarch64/libconnector.so https://github.com/aikaterna/lavaplayer-natives/raw/master/linux-aarch64/libconnector.so && \
    zip traq-music-station.jar natives/linux-aarch64/libconnector.so && \
    rm -r natives

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dwebdriver.chrome.driver=/usr/bin/chromedriver", "./traq-music-station.jar"]

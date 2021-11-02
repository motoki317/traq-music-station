FROM maven:3.8.3-openjdk-17-slim AS build

WORKDIR /usr/src/app

COPY ./pom.xml ./
# Resole dependencies
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:resolve

COPY ./src ./src
# https://stackoverflow.com/questions/61301818/java-failed-to-exec-spawn-helper-error-since-moving-to-java-14-on-linux
RUN --mount=type=cache,target=/root/.m2 mvn -B package -D skipTests -D jdk.lang.Process.launchMechanism=vfork

FROM openjdk:17-slim AS runtime

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

ENV CHROME_BIN /usr/bin/chromium
ENTRYPOINT ["java", "-jar", "-Dwebdriver.chrome.driver=/usr/bin/chromedriver", "./traq-music-station.jar"]

FROM maven:3.6.3-jdk-11 AS build

WORKDIR /usr/src/app

COPY ./pom.xml ./
# Resole dependencies
RUN --mount=type=cache,target=/root/.m2 mvn dependency:resolve

COPY ./src ./src
RUN --mount=type=cache,target=/root/.m2 mvn package -D skipTests

FROM openjdk:11-jre-slim AS runtime

RUN apt-get update && \
    apt-get install -y wget unzip
# install google chrome
RUN wget -O /tmp/google-chrome.deb "https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb" && \
    apt-get install -y /tmp/google-chrome.deb && \
    rm /tmp/google-chrome.deb
# download chrome driver
RUN wget -O /tmp/chromedriver_linux64.zip https://chromedriver.storage.googleapis.com/88.0.4324.96/chromedriver_linux64.zip && \
    unzip /tmp/chromedriver_linux64.zip -d /tmp/google-chrome && \
    rm /tmp/chromedriver_linux64.zip && \
    mv /tmp/google-chrome/chromedriver /usr/bin/chromedriver

WORKDIR /usr/src/app

COPY --from=build /usr/src/app/src/main/assets/* ./
COPY --from=build /usr/src/app/target/traq-music-station-jar-with-dependencies.jar ./traq-music-station.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dwebdriver.chrome.driver=/usr/bin/chromedriver", "./traq-music-station.jar"]

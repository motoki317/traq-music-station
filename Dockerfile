FROM maven:3.6.3-jdk-11 AS build

WORKDIR /usr/src/app

COPY ./pom.xml ./
# Resole app dependencies
RUN mvn dependency:resolve
# Resolve build dependencies
RUN mvn package

COPY ./src ./src
RUN mvn package -D skipTests

FROM openjdk:11-jre-slim AS runtime

RUN apt-get update && \
    apt-get install -y wget unzip
# install google chrome
# NOTE: intentionally using the older version, because upgrading from 84 to 86 somehow broke the ogg-opus stream reading for "Audio" input
RUN wget -O /tmp/google-chrome-stable_current_amd64.deb "https://www.slimjet.com/chrome/download-chrome.php?file=files%2F84.0.4147.135%2Fgoogle-chrome-stable_current_amd64.deb" && \
    apt-get install -y /tmp/google-chrome-stable_current_amd64.deb && \
    rm /tmp/google-chrome-stable_current_amd64.deb
# download chrome driver
RUN wget -O /tmp/chromedriver_linux64.zip https://chromedriver.storage.googleapis.com/84.0.4147.30/chromedriver_linux64.zip && \
    unzip /tmp/chromedriver_linux64.zip -d /tmp/google-chrome && \
    rm /tmp/chromedriver_linux64.zip && \
    mv /tmp/google-chrome/chromedriver /usr/bin/chromedriver

WORKDIR /usr/src/app

COPY --from=build /usr/src/app/src/main/assets/* ./
COPY --from=build /usr/src/app/target/traq-music-station-jar-with-dependencies.jar ./traq-music-station.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dwebdriver.chrome.driver=/usr/bin/chromedriver", "./traq-music-station.jar"]

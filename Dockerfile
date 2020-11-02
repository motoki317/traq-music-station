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

RUN apt-get update && apt-get install -y wget unzip
# install google chrome
RUN wget -O /tmp/google-chrome-stable_current_amd64.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
RUN apt-get install -y /tmp/google-chrome-stable_current_amd64.deb
# install chrome driver
RUN wget -O /tmp/chromedriver_linux64.zip https://chromedriver.storage.googleapis.com/86.0.4240.22/chromedriver_linux64.zip
RUN unzip /tmp/chromedriver_linux64.zip -d /tmp/google-chrome && rm /tmp/chromedriver_linux64.zip
RUN mv /tmp/google-chrome/chromedriver /usr/bin/chromedriver

WORKDIR /usr/src/app

COPY --from=build /usr/src/app/src/main/assets/* ./
COPY --from=build /usr/src/app/target/traq-music-station-jar-with-dependencies.jar ./traq-music-station.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dwebdriver.chrome.driver=/usr/bin/chromedriver", "./traq-music-station.jar"]

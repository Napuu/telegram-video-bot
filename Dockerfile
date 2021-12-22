FROM clojure:openjdk-18-lein-slim-bullseye
RUN apt-get update -y && apt-get install youtube-dl curl -y
COPY . .
RUN lein uberjar
CMD java -jar /tmp/target/uberjar/telegram-video-download-bot*-standalone.jar

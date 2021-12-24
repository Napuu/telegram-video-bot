FROM clojure:openjdk-18-lein-slim-bullseye
RUN apt-get update -y && apt-get install curl ffmpeg python3 -y
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp
COPY . .
RUN lein uberjar
CMD java -jar /tmp/target/uberjar/telegram-video-download-bot*-standalone.jar

FROM clojure:openjdk-18-lein-slim-bullseye as clojure-build
COPY . .
RUN lein uberjar
RUN mv /tmp/target/uberjar/telegram-video-download-bot*-standalone.jar /tmp/out.jar

FROM clojure:openjdk-18-lein-slim-bullseye as yt-dlp-build
RUN apt update -y && apt-get install python3 build-essential zip git -y
RUN cd /tmp && git clone https://github.com/yt-dlp/yt-dlp
RUN cd /tmp/yt-dlp && make yt-dlp && mkdir -p /usr/local/bin && mv yt-dlp /usr/local/bin/

FROM clojure:openjdk-18-lein-slim-bullseye as run
COPY --from=yt-dlp-build /usr/local/bin/yt-dlp /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp
RUN apt-get update && apt-get install python3 ffmpeg -y
WORKDIR /app
COPY --from=clojure-build /tmp/out.jar .

ENTRYPOINT ["java", "-jar", "out.jar"]

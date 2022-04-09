FROM clojure:openjdk-18-lein-slim-bullseye as clojure-build

RUN apt-get update && apt-get install curl ffmpeg python3 -y
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp
RUN chmod a+rx /usr/local/bin/yt-dlp
COPY . .
RUN lein uberjar
RUN mv /tmp/target/uberjar/telegram-video-download-bot*-standalone.jar /tmp/out.jar

FROM clojure:openjdk-18-lein-slim-bullseye as yt-dlp-build
RUN apt-get update && apt-get install python3 build-essential zip git -y
RUN cd /tmp && git clone https://github.com/yt-dlp/yt-dlp
RUN cd /tmp/yt-dlp && make yt-dlp && mkdir -p /usr/local/bin && mv yt-dlp /usr/local/bin/

FROM clojure:openjdk-18-lein-slim-bullseye as run
WORKDIR /app
COPY --from=clojure-build /tmp/out.jar .
COPY --from=yt-dlp-build /usr/local/bin/yt-dlp /usr/local/bin/yt-dlp

CMD java -jar out.jar

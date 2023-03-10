FROM clojure:temurin-19-lein
COPY . .
RUN lein uberjar
RUN mv /tmp/target/uberjar/telegram-video-download-bot*-standalone.jar /tmp/out.jar

RUN apt-get update -y && apt-get install python3 build-essential zip git -y
RUN cd /tmp && git clone https://github.com/yt-dlp/yt-dlp
RUN cd /tmp/yt-dlp && make yt-dlp && mkdir -p /usr/local/bin && mv yt-dlp /usr/local/bin/

RUN chmod a+rx /usr/local/bin/yt-dlp
RUN apt-get update -y && apt-get install python3 ffmpeg -y
WORKDIR /app
RUN mv /tmp/out.jar .

ENTRYPOINT ["java", "-jar", "out.jar"]

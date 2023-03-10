FROM clojure:temurin-19-lein
WORKDIR /app
COPY . .

RUN apt-get update -y && apt-get install python3 build-essential zip git ffmpeg -y
RUN cd /tmp && git clone https://github.com/yt-dlp/yt-dlp --depth=1
RUN cd /tmp/yt-dlp && make yt-dlp && mkdir -p /usr/local/bin && mv yt-dlp /usr/local/bin/

RUN chmod a+rx /usr/local/bin/yt-dlp

ENTRYPOINT ["lein"]

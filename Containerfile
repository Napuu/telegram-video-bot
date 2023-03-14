FROM nimlang/nim
WORKDIR /app
COPY . .
RUN nim c --gc:arc --threads:on -d:ssl -d:release --out=bot src/bot.nim
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get -qq update && apt-get -qq install zip git ffmpeg build-essential python3 > /dev/null
RUN cd /tmp && git clone https://github.com/yt-dlp/yt-dlp
RUN cd /tmp/yt-dlp && make yt-dlp && mkdir -p /usr/local/bin && mv yt-dlp /usr/local/bin/
ENV YTDLP_LOCATION=/usr/local/bin/yt-dlp
ENV FFPROBE_LOCATION=/usr/bin/ffprobe
RUN chmod a+rx /usr/local/bin/yt-dlp
CMD ["/app/run"]

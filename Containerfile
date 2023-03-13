from ubuntu:rolling
RUN apt-get update -qq && apt-get install -y zip git ffmpeg build-essential nim python3
WORKDIR /app
COPY . .
RUN nim c --gc:arc --threads:on -d:ssl -d:release run.nim
RUN cd /tmp && git clone https://github.com/yt-dlp/yt-dlp
RUN cd /tmp/yt-dlp && make yt-dlp && mkdir -p /usr/local/bin && mv yt-dlp /usr/local/bin/

RUN chmod a+rx /usr/local/bin/yt-dlp

ENTRYPOINT ["run"]

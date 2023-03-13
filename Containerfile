from ubuntu:rolling
RUN apt-get update -qq && apt-get install -y zip git ffmpeg build-essential python3 curl
WORKDIR /app
RUN curl https://nim-lang.org/download/nim-1.6.12-linux_x64.tar.xz > nim.tar.xz
RUN tar -xvf nim.tar.xz && mv ./nim*/bin/* /usr/bin/ && mv ./nim*/lib/* /usr/lib/
COPY . .
RUN nim c --gc:arc --threads:on -d:ssl -d:release run.nim
RUN cd /tmp && git clone https://github.com/yt-dlp/yt-dlp
RUN cd /tmp/yt-dlp && make yt-dlp && mkdir -p /usr/local/bin && mv yt-dlp /usr/local/bin/

RUN chmod a+rx /usr/local/bin/yt-dlp

ENTRYPOINT ["run"]

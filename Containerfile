from ubuntu:rolling
RUN apt-get update -qq && apt-get install -y zip git ffmpeg build-essential leiningen 
# WORKDIR /app
# COPY . .

# RUN apt-get update -y && apt-get install python3 build-essential zip git ffmpeg -y
# RUN cd /tmp && git clone https://github.com/yt-dlp/yt-dlp
# RUN cd /tmp/yt-dlp && make yt-dlp && mkdir -p /usr/local/bin && mv yt-dlp /usr/local/bin/

# RUN chmod a+rx /usr/local/bin/yt-dlp

# ENTRYPOINT ["lein"]

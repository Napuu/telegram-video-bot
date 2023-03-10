FROM clojure:temurin-19-lein
WORKDIR /app

ENTRYPOINT ["lein"]

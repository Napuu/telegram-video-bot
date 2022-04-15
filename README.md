# telegram-video-download-bot

Telegram Bot for downloading videos sent to group chats. The original message is also deleted, so chat history doesn't become cluttered.  

See it in action!  
![demo gif](demo.gif)

## Setting up

1. Have this bot running somewhere. See [Bots: An introduction for developers](https://core.telegram.org/bots) for more instructions.
2. Set group privacy off (bot reads only messages ending " dl").
3. Invite bot to group you want.
4. Post a message that contains link and ends with " dl", e.g, `https://www.youtube.com/watch?v=9S8eNZ4fw5I dl`.

## Running

Put correct token in `config.edn`.
### With Leiningen

This expects following executables to be available at current PATH `curl` and `yt-dlp`.  

Run `podman-compose up message-queue` to start message queue, then simply `lein run`. [Leiningen](https://leiningen.org/) needed.

### With Podman

```
podman-compose up
```


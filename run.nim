import asyncdispatch, logging, options, strutils, os, osproc, std/sequtils, std/streams, oids, threadpool, asyncfutures, std/httpclient, std/mimetypes, std/json, std/tables, std/sugar
import client
var L = newConsoleLogger(fmtStr="$levelname, [$time] ")
addHandler(L)


let API_KEY = getEnv("BOT_TOKEN")

let teClient = newTeClient(API_KEY)

var threadsAlive = 0
const CONCURRENCY_LIMIT = 2

proc ytdlpBaseArgs(output: string): seq[string] = @["--merge-output-format", "mp4",
                       "--max-filesize", "500m",
                       "-f", "((bv*[fps>30]/bv*)[height<=720]/(wv*[fps>30]/wv*)) + ba / (b[fps>30]/b)[height<=720]/(w[fps>30]/w)",
                        "-o", output,
                        # "--rate-limit", "0.5M",
                       "-S", "codec:h264"]

proc peekStd(p: Process): void =
  for line in peekableOutputStream(p).lines:
    echo "output: ", line
  for line in peekableErrorStream(p).lines:
    echo "error: ", line

proc downloadVideoAndSend(url: string, chatId: int): Future[string] {.async gcsafe.} =
  echo "starting download", threadsAlive
  while threadsAlive >= CONCURRENCY_LIMIT:
    echo "waiting for free worker...", $threadsAlive
    sleep(200)

  inc threadsAlive
  let tmpFile = $genOid() & ".mp4"
  echo "starting dl for ", tmpFile
  let args = concat(@[url], ytdlpBaseArgs(tmpFile))
  let p = startProcess("/opt/homebrew/bin/yt-dlp", args=args)
  let exitCode = waitForExit(p)

  if true:
    for line in peekableOutputStream(p).lines:
      echo "output: ", line
    for line in peekableErrorStream(p).lines:
      echo "error: ", line
    echo "exitcode", exitCode
  echo "done, sending next"
  
  let fullFilePath = getAppDir() & "/" & tmpFile
  # let fullFilePath = "file://" & getAppDir() & "/" & tmpFile

  echo "full file path: ", fullFilePath
  {.cast(gcsafe).}:
    let mimes = newMimetypes()
    var client = newHttpClient()
    var data = newMultipartData()
    data.addFiles({"video": fullFilePath}, mimeDb = mimes)
    echo client.postContent("https://api.telegram.org/bot" & API_KEY & "/sendVideo?chat_id=" & $chatId, multipart=data)
  #
  # echo "sending done"
  dec threadsAlive

# proc act(f: string) =
#   echo "from act: ", f

# proc sendVideo(f: string, chatId: string, b: Telebot): Future[void] {.async.} =
#   let fullFilePath = "file://" & getAppDir() & "/" & f
#   echo "full file path: ", fullFilePath
#   discard await b.sendVideo(chatId, fullFilePath)

# proc updateHandler(b: Telebot, u: Update): Future[bool] {.async gcsafe.} =
#   if not u.message.isSome:
#     # return true will make bot stop process other callbacks
#     return true
#   var response = u.message.get

#   if response.text.isSome:
#     let text = response.text.get
#     let endingString = " dl"
#     if not text.endsWith(endingString):
#       return true
#     let url = text[0..^(endingString.len+1)]
#     echo "???url", url

#     discard await b.sendMessage($response.chat.id, text, disableNotification = true)
#     discard await b.deleteMessage($response.chat.id, response.message_id)
#     # discard await b.sendVideo($response.chat.id, this_file)
#     discard spawn downloadVideoAndSend(url, $response.chat.id)
    # discard await b.sendVideo($response.chat.id, "file:///Users/santeri/projects/telegram-video-download-bot/640ca7a38794c85457767735.mp4")
    # discard await b.sendVideo($response.chat.id, "file:///Users/santeri/projects/telegram-video-download-bot/640c944d99651aba359c0cd5.mp4")
    # discard spawn downloadVideoAndSend(url, $response.chat.id, b)
    # asyncCheck f
    # while not f.isReady:
    #   echo "Not ready ", url
    #   sleep(100)



proc handleUpdate(update: JsonNode): void =
  if update.hasKey("message") and update["message"].hasKey("text"):
    let
      msg = update["message"]
      msgText = msg["text"].getStr
      chatId = msg["chat"]["id"].getInt
      msgId = msg["message_id"].getInt
      endingString = " dl"
    if msgText.endsWith(endingString):
      let url = msgText[0..^(endingString.len+1)]
      # discard downloadVideoAndSend(url, chatId)
      teClient.deleteMessage(chatId, msgId)

proc main() =
  var lastUpdateId = -1
  while true:
    for update in teClient.getUpdates(lastUpdateId)["result"]:
      handleUpdate(update)
      lastUpdateId = update["update_id"].getInt + 1

main()

import std/[sugar, locks, options, math, logging, strutils, os, threadpool, json, tables]
import client, util

var L = newConsoleLogger(levelThreshold=getLogLevel(), fmtStr="$levelname, [$time] ")
addHandler(L)
# something strange is happening at containers
# and logs are not printed??
echo "Init logger"

var chatsSendingLock: Lock
initLock(chatsSendingLock)
var chatsSending {.guard: chatsSendingLock.}: Table[int, int]

const CONCURRENCY_LIMIT = 2

proc handleVideoThreaded(chatId: int, url: string, msgId: int, replyToMsgId: Option[int]): void {.gcsafe.} =
  {.cast(gcsafe).}:
    while true:
      withLock chatsSendingLock:
        let runnersAlive = collect(for v in chatsSending.values: v).sum
        if runnersAlive < CONCURRENCY_LIMIT:
          break
        L.log(lvlNotice, "waiting for free worker... ", $runnersAlive)
      sleep(2000)

    withLock chatsSendingLock:
      if (chatsSending.hasKey(chatId)):
        chatsSending[chatId].inc
      else:
        chatsSending[chatId] = 1

    var resp: Option[JsonNode]
    resp = telegramRequest("sendChatAction", %*{"chat_id": chatId, "action": "upload_video"})
    if resp.isOk:
      L.log(lvlDebug, "Initial status msg sent successfully")
    else:
      L.log(lvlWarn, "Sending initial status failed")
    let (tmpFile, statusCode) = downloadVideo(url)

    if statusCode == 0:
      L.log(lvlDebug, "Downloaded video successfully")
      let fullFilePath = getAppDir() & "/" & tmpFile
      let dimensions = getVideoDimensions(fullFilePath)
      var videoOptions = %*{"chat_id": chatId, "width": dimensions[0], "height": dimensions[1]}
      resp = telegramVideoRequest(videoOptions, fullFilePath)
      if resp.isOk:
        L.log(lvlDebug, "Sending video succeeded")
      else:
        L.log(lvlWarn, "Sending video failed")
      resp = telegramRequest("deleteMessage", %*{"chat_id": chatId, "message_id": msgId})
      if resp.isOk:
        L.log(lvlDebug, "Deleting message succeeded")
      else:
        L.log(lvlWarn, "Deleting message failed")
    else:
      L.log(lvlWarn, "Downloading video failed, url: ", url)
      resp = telegramRequest("sendMessage", %*{"chat_id": chatId, "text": "Hyvä linkki..."})
      if resp.isOk:
        L.log(lvlDebug, "Sending message succeeded")
      else:
        L.log(lvlWarn, "Deleting message failed")

    withLock chatsSendingLock:
      chatsSending[chatId].dec

proc sendingStatusPoller(): void {.gcsafe.} =
  {.cast(gcsafe).}:
    while true:
      withLock chatsSendingLock:
        for chatId in chatsSending.keys:
          if (chatsSending[chatId] > 0):
            let resp = telegramRequest("sendChatAction", %*{"chat_id": chatId, "action": "upload_video"})
            if resp.isOk:
              L.log(lvlDebug, "Polling status msg sent successfully")
            else:
              L.log(lvlWarn, "Sending polling status failed")
      sleep 4000
  
proc handleUpdate(update: JsonNode): void =
  if update.hasKey("message") and update["message"].hasKey("text"):
    let
      msg = update["message"]
      msgText = msg["text"].getStr
      chatId = msg["chat"]["id"].getInt
      msgId = msg["message_id"].getInt
      replyToMsgId = if msg.hasKey("reply_to_message"): some(msg["reply_to_message"]["message_id"].getInt) else: none(int)
      endingString = " dl"
    L.log(lvlDebug, "Received update")
    if msgText.endsWith(endingString):
      let url = msgText[0..^(endingString.len+1)]
      spawn handleVideoThreaded(chatId, url, msgId, replyToMsgId)

proc main() =
  var lastUpdateId = -1
  spawn sendingStatusPoller()
  while true:
    let updates = telegramRequest("getUpdates", %*{"timeout": 60, "offset": lastUpdateId})
    if updates.isNone:
      warn "Getting update failed"
      sleep 5000
    else:
      for update in updates.get()["result"]:
        handleUpdate(update)
        lastUpdateId = update["update_id"].getInt + 1

main()

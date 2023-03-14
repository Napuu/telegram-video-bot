import std/[sugar, locks, options, math, strutils, os, threadpool, json, tables]
import client, util

# let levelThreshold = getLogLevel()
# echo "Init logger for level ", levelThreshold
# setLogLevel(levelThreshold)

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
        echo("NOTICE: waiting for free worker... ", runnersAlive)
      sleep(2000)

    withLock chatsSendingLock:
      if (chatsSending.hasKey(chatId)):
        chatsSending[chatId].inc
      else:
        chatsSending[chatId] = 1

    var resp: Option[JsonNode]
    resp = telegramRequest("sendChatAction", %*{"chat_id": chatId, "action": "upload_video"})
    if resp.isOk:
      echo("DEBUG: Initial status msg sent successfully")
    else:
      echo("WARN: Sending initial status failed")
    let (tmpFile, statusCode) = downloadVideo(url)

    if statusCode == 0:
      echo("DEBUG: Downloaded video successfully")
      let fullFilePath = getAppDir() & "/" & tmpFile
      let dimensions = getVideoDimensions(fullFilePath)
      var videoOptions = %*{"chat_id": chatId, "width": dimensions[0], "height": dimensions[1]}
      resp = telegramVideoRequest(videoOptions, fullFilePath)
      if resp.isOk:
        echo("DEBUG: Sending video succeeded")
      else:
        echo("WARN: Sending video failed")
      resp = telegramRequest("deleteMessage", %*{"chat_id": chatId, "message_id": msgId})
      if resp.isOk:
        echo("DEBUG: Deleting message succeeded")
      else:
        echo("WARN: Deleting message failed")
    else:
      echo("WARN: Downloading video failed, url: ", url)
      resp = telegramRequest("sendMessage", %*{"chat_id": chatId, "text": "Hyvä linkki..."})
      if resp.isOk:
        echo("DEBUG: Sending message succeeded")
      else:
        echo("WARN: Deleting message failed")

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
              echo("DEBUG: Polling status msg sent successfully")
            else:
              echo("WARN: Sending polling status failed")
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
    echo("DEBUG: Received update")
    if msgText.endsWith(endingString):
      let url = msgText[0..^(endingString.len+1)]
      spawn handleVideoThreaded(chatId, url, msgId, replyToMsgId)

proc main() =
  var lastUpdateId = -1
  spawn sendingStatusPoller()
  while true:
    let updates = telegramRequest("getUpdates", %*{"timeout": 60, "offset": lastUpdateId})
    if updates.isNone:
      echo("WARN: Getting update failed")
      sleep 5000
    else:
      for update in updates.get()["result"]:
        handleUpdate(update)
        lastUpdateId = update["update_id"].getInt + 1

main()

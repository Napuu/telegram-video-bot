import std/[sugar, locks, options, math, strutils, os, threadpool, json, tables, logging]
import client, util

initConsoleLogger()

var chatsSendingLock: Lock
initLock(chatsSendingLock)
var chatsSending {.guard: chatsSendingLock.}: Table[int, int]

const CONCURRENCY_LIMIT = 2

proc handleVideoThreaded(chatId: int, url: string, msgId: int, replyToMsgId: Option[int]): void {.gcsafe.} =
  initConsoleLogger()
  {.cast(gcsafe).}:
    while true:
      withLock chatsSendingLock:
        let runnersAlive = collect(for v in chatsSending.values: v).sum
        if runnersAlive < CONCURRENCY_LIMIT:
          break
        notice("Waiting for free worker... ", runnersAlive)
      sleep(2000)

    withLock chatsSendingLock:
      if (chatsSending.hasKey(chatId)):
        chatsSending[chatId].inc
      else:
        chatsSending[chatId] = 1

    var resp: Option[JsonNode]
    resp = telegramRequest("sendChatAction", %*{"chat_id": chatId, "action": "upload_video"})
    if resp.isOk:
      debug("Initial status msg sent successfully")
    else:
      warn("Sending initial status failed")
    let (tmpFile, statusCode) = downloadVideo(url)

    var sendingVideoSucceeded = false
    if statusCode == 0:
      debug("Downloaded video successfully")
      let dimensions = getVideoDimensions(tmpFile)
      var videoOptions = %*{"chat_id": chatId, "width": dimensions[0], "height": dimensions[1]}
      if replyToMsgId.isSome:
        videoOptions["reply_to_message_id"] = %*(replyToMsgId.get())
      resp = telegramVideoRequest(videoOptions, tmpFile)
      if resp.isOk:
        debug("Sending video succeeded")
        sendingVideoSucceeded = true
      else:
        warn("WARN: Sending video failed")

    if sendingVideoSucceeded:
      resp = telegramRequest("deleteMessage", %*{"chat_id": chatId, "message_id": msgId})
      if resp.isOk:
        debug("Deleting message succeeded")
      else:
        warn("Deleting message failed")
    else:
      warn("Sending video failed, url: ", url)
      resp = telegramRequest("sendMessage", %*{"chat_id": chatId, "text": "Hyvä linkki...", "reply_to_message_id": msgId})
      if resp.isOk:
        debug("Sending error message succeeded")
      else:
        warn("Sending error message failed")

    withLock chatsSendingLock:
      chatsSending[chatId].dec

proc sendingStatusPoller(): void {.gcsafe.} =
  initConsoleLogger()
  {.cast(gcsafe).}:
    while true:
      withLock chatsSendingLock:
        for chatId in chatsSending.keys:
          if (chatsSending[chatId] > 0):
            let resp = telegramRequest("sendChatAction", %*{"chat_id": chatId, "action": "upload_video"})
            if resp.isOk:
              debug("Polling status msg sent successfully")
            else:
              warn("Sending polling status failed")
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
    debug("Received update")
    if msgText.endsWith(endingString):
      let url = msgText[0..^(endingString.len+1)]
      spawn handleVideoThreaded(chatId, url, msgId, replyToMsgId)

proc main() =
  var lastUpdateId = -1
  spawn sendingStatusPoller()
  while true:
    let updates = telegramRequest("getUpdates", %*{"timeout": 60, "offset": lastUpdateId})
    if updates.isNone:
      warn("Getting update failed")
      sleep 5000
    else:
      for update in updates.get()["result"]:
        handleUpdate(update)
        lastUpdateId = update["update_id"].getInt + 1

main()

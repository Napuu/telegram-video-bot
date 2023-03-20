import options, strutils, std/[httpclient,json,sugar,mimetypes,os]
import ./util

type
  TeClient* = object
    token: string
    slowPollTimeoutSeconds: int
    httpClient: HttpClient
    baseUrl: string

proc newTeClient*(token: string): TeClient =
  TeClient(
    token: token,
    slowPollTimeoutSeconds:  60,
    httpClient: newHttpClient(),
    baseUrl: "https://api.telegram.org/bot" & token
  )

proc getUpdates*(teClient: TeClient, lastUpdateId: int): JsonNode =
  let
    queryParamsString = toQueryParamsString %*{
      "timeout": teClient.slowPollTimeoutSeconds,
      "offset": lastUpdateId
    }
    resp = newHttpClient().getContent(teClient.baseUrl & "/getUpdates" & "?" & queryParamsString)
  parseJson(resp)

proc deleteMessage*(teClient: TeClient, chatId: int, messageId: int): void =
  let
    queryParamsString = toQueryParamsString %*{
      "chat_id": chatId,
      "message_id": messageId
    }
  
  discard newHttpClient().get(teClient.baseUrl & "/deleteMessage" & "?" & queryParamsString)

proc isOk*(r: Option[JsonNode]): bool =
  if r.isSome and r.get.hasKey("ok") and r.get["ok"].getBool == true:
    true
  else:
    false

proc telegramRequest*(action: string, body: JsonNode): Option[JsonNode] =
  try:
    var client = newHttpClient()
    client.headers = newHttpHeaders({ "Content-Type": "application/json" })
    let response = client.postContent("https://api.telegram.org/bot" & getEnv("BOT_TOKEN") & "/" & action, body = $body)
    result = some(parseJson(response))
  except:
    result = none(JsonNode)

proc telegramVideoRequest*(body: JsonNode, filePath: string): Option[JsonNode] =
  try:
    var data = newMultipartData()
    data.addFiles({"video": filePath}, mimeDb = newMimetypes())
    let response = newHttpClient().postContent("https://api.telegram.org/bot" & getEnv("BOT_TOKEN") & "/sendVideo?" & body.toQueryParamsString, multipart = data)
    result = some(parseJson(response))
  except:
    result = none(JsonNode)

proc sendMessage*(teClient: TeClient, chatId: int, text: string): void =
  let
    queryParamsString = toQueryParamsString %*{
      "chat_id": chatId,
      "text": text
    }
  discard newHttpClient().get(teClient.baseUrl & "/sendMessage" & "?" & queryParamsString)

proc sendVideo*(teClient: TeClient, chatId: int, path: string, width: int, height: int): void =
  let
    queryParamsString = toQueryParamsString %*{
      "chat_id": chatId,
      "width": width,
      "height": height
    }
    mimes = newMimetypes()
  var data = newMultipartData()
  data.addFiles({"video": path}, mimeDb = mimes)
  discard newHttpClient().postContent(teClient.baseUrl & "/sendVideo" & "?" & queryParamsString, multipart = data)

proc sendChatAction*(teClient: TeClient, chatId: int, action: string): void =
  let
    queryParamsString = toQueryParamsString %*{
      "chat_id": chatId,
      "action": action
    }
  discard teClient.httpClient.post(teClient.baseUrl & "/sendChatAction" & "?" & queryParamsString)

import options, strutils, std/httpclient, std/json, std/sugar
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
    queryParamsString = {
      "timeout": teClient.slowPollTimeoutSeconds,
      "offset": lastUpdateId
    }.toQueryParamsString
    resp = teClient.httpClient.getContent(teClient.baseUrl & "/getUpdates" & "?" & queryParamsString)
  parseJson(resp)

proc deleteMessage*(teClient: TeClient, chatId: int, messageId: int): void =
  let
    queryParamsString = {
      "chat_id": chatId,
      "message_id": messageId
    }.toQueryParamsString
  discard teClient.httpClient.get(teClient.baseUrl & "/deleteMessage" & "?" & queryParamsString)

import std/[macros, osproc, strutils, streams, sequtils, sugar, oids, os]

# https://stackoverflow.com/a/29585066/1550017
proc getVideoDimensions*(path: string): (int, int) =
  let
    args = ["-v", "error", "-select_streams", "v", "-show_entries", "stream=width,height", "-of", "csv=p=0:s=x", path]
    p = startProcess(getEnv("FFPROBE_LOCATION"), args=args)
    output = collect(for line in peekableOutputStream(p).lines: line)[0]
    outputSplit = output.split("x")
    width = outputSplit[0].parseInt
    height = outputSplit[1].parseInt
  (width, height)

proc ytdlpBaseArgs(output: string): seq[string] = @["--merge-output-format", "mp4",
                       "--max-filesize", "500m",
                       "-f", "((bv*[fps>30]/bv*)[height<=720]/(wv*[fps>30]/wv*)) + ba / (b[fps>30]/b)[height<=720]/(w[fps>30]/w)",
                        "-o", output,
                        # "--rate-limit", "0.5M",
                       "-S", "codec:h264"]

proc downloadVideo*(url: string): (string, int) =
  let tmpFile = "/tmp/" & $genOid() & ".mp4"
  let args = concat(@[url], ytdlpBaseArgs(tmpFile))
  let p = startProcess(getEnv("YTDLP_LOCATION"), args=args)
  let exitCode = waitForExit(p)
  (tmpFile, exitCode)

# proc getLogLevel*(): LogLevel =
#   let level = getEnv("LOG_LEVEL")
#   if level == "DEBUG": DEBUG
#   else: NOTICE

macro toQueryParamsString*(s: untyped): untyped =
  nnkStmtList.newTree(
    nnkLetSection.newTree(
      nnkIdentDefs.newTree(
        newIdentNode("params"),
        newEmptyNode(),
        nnkCall.newTree(
          newIdentNode("collect"),
          nnkStmtList.newTree(
            nnkForStmt.newTree(
              newIdentNode("k"),
              nnkDotExpr.newTree(
                s,
                newIdentNode("keys")
              ),
              nnkStmtList.newTree(
                nnkInfix.newTree(
                  newIdentNode("&"),
                  nnkInfix.newTree(
                    newIdentNode("&"),
                    nnkPrefix.newTree(
                      newIdentNode("$"),
                      newIdentNode("k")
                    ),
                    newLit("=")
                  ),
                  nnkStmtListExpr.newTree(
                    nnkIfStmt.newTree(
                      nnkElifExpr.newTree(
                        nnkInfix.newTree(
                          newIdentNode("=="),
                          nnkDotExpr.newTree(
                            nnkBracketExpr.newTree(
                              s,
                              newIdentNode("k")
                            ),
                            newIdentNode("kind")
                          ),
                          newIdentNode("JString")
                        ),
                        nnkDotExpr.newTree(
                          nnkBracketExpr.newTree(
                            s,
                            newIdentNode("k")
                          ),
                          newIdentNode("getStr")
                        )
                      ),
                      nnkElifExpr.newTree(
                        nnkInfix.newTree(
                          newIdentNode("=="),
                          nnkDotExpr.newTree(
                            nnkBracketExpr.newTree(
                              s,
                              newIdentNode("k")
                            ),
                            newIdentNode("kind")
                          ),
                          newIdentNode("JFloat")
                        ),
                        nnkPrefix.newTree(
                          newIdentNode("$"),
                          nnkDotExpr.newTree(
                            nnkBracketExpr.newTree(
                              s,
                              newIdentNode("k")
                            ),
                            newIdentNode("getFloat")
                          )
                        )
                      ),
                      nnkElifExpr.newTree(
                        nnkInfix.newTree(
                          newIdentNode("=="),
                          nnkDotExpr.newTree(
                            nnkBracketExpr.newTree(
                              s,
                              newIdentNode("k")
                            ),
                            newIdentNode("kind")
                          ),
                          newIdentNode("JBool")
                        ),
                        nnkPrefix.newTree(
                          newIdentNode("$"),
                          nnkDotExpr.newTree(
                            nnkBracketExpr.newTree(
                              s,
                              newIdentNode("k")
                            ),
                            newIdentNode("getBool")
                          )
                        )
                      ),
                      nnkElseExpr.newTree(
                        nnkPrefix.newTree(
                          newIdentNode("$"),
                          nnkDotExpr.newTree(
                            nnkBracketExpr.newTree(
                              s,
                              newIdentNode("k")
                            ),
                            newIdentNode("getInt")
                          )
                        )
                      )
                    )
                  )
                )
              )
            )
          )
        )
      )
    ),
    nnkCall.newTree(
      nnkDotExpr.newTree(
        newIdentNode("params"),
        newIdentNode("join")
      ),
      newLit("&")
    )
  )

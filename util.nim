import std/[macros]

# Just trying out...
macro toQueryParamsString*(s: untyped): untyped =
  result = nnkStmtList.newTree(
    nnkCall.newTree(
      nnkDotExpr.newTree(
        nnkCall.newTree(
          newIdentNode("collect"),
          nnkForStmt.newTree(
            newIdentNode("pair"),
            s,
            nnkStmtList.newTree(
              nnkInfix.newTree(
                newIdentNode("&"),
                nnkInfix.newTree(
                  newIdentNode("&"),
                  nnkBracketExpr.newTree(
                    newIdentNode("pair"),
                    newLit(0)
                  ),
                  newLit("=")
                ),
                nnkPrefix.newTree(
                  newIdentNode("$"),
                  nnkBracketExpr.newTree(
                    newIdentNode("pair"),
                    newLit(1)
                  )
                )
              )
            )
          )
        ),
        newIdentNode("join")
      ),
      newLit("&")
    )
  )

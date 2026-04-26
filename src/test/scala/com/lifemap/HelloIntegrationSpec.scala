package com.lifemap

import zio._
import zio.http._
import zio.test._

object HelloIntegrationSpec extends ZIOSpecDefault:

  def spec = suite("Hello Route - Integration")(
    test("GET / returns 200 OK with Hello World text") {
      for
        port     <- Server.install(Main.routes)
        client   <- ZIO.service[Client]
        response <- client(Request.get(url"http://localhost:$port/"))
        body     <- response.body.asString
      yield assertTrue(response.status == Status.Ok) &&
        assertTrue(body == "Hello, World!")
    }
  ).provide(
    Server.defaultWithPort(0),
    Client.default,
    Scope.default,
  )

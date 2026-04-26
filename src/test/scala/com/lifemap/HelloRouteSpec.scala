package com.lifemap

import zio._
import zio.http._
import zio.test._

object HelloRouteSpec extends ZIOSpecDefault:

  def spec = suite("Hello Route")(
    test("GET / returns 200 OK") {
      for
        response <- Main.routes(Request.get(url"http://localhost/"))
        body     <- response.body.asString
      yield assertTrue(response.status == Status.Ok) &&
        assertTrue(body == "Hello, World!")
    }
  )

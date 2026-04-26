package com.lifemap

import zio._
import zio.http._

object Main extends ZIOAppDefault:

  val routes = Routes(
    Method.GET / "" -> handler(Response.text("Hello, World!"))
  )

  override def run =
    Server.serve(routes).provide(Server.default)

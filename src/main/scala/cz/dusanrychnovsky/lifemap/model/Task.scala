package cz.dusanrychnovsky.lifemap.model

import zio.json._
import java.util.UUID

enum TaskStatus:
  case New, Active, Completed, Removed

object TaskStatus:
  given JsonEncoder[TaskStatus] = JsonEncoder[String].contramap:
    case New       => "new"
    case Active    => "active"
    case Completed => "completed"
    case Removed   => "removed"

  given JsonDecoder[TaskStatus] = JsonDecoder[String].mapOrFail:
    case "new"       => Right(New)
    case "active"    => Right(Active)
    case "completed" => Right(Completed)
    case "removed"   => Right(Removed)
    case other       => Left(s"Unknown status: '$other'")

case class Task(id: UUID, title: String, description: String, status: TaskStatus)

object Task:
  given JsonEncoder[UUID] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[UUID] = JsonDecoder[String].mapOrFail: s =>
    try Right(UUID.fromString(s))
    catch case _: IllegalArgumentException => Left(s"Invalid UUID: '$s'")

  given JsonEncoder[Task] = DeriveJsonEncoder.gen[Task]
  given JsonDecoder[Task] = DeriveJsonDecoder.gen[Task]

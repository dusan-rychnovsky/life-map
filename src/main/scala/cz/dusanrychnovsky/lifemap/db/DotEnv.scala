package cz.dusanrychnovsky.lifemap.db

import java.io.File
import scala.io.Source

/** Loads key/value pairs from a `.env` file in the working directory.
  *
  * Real OS environment variables always win over `.env` entries, so production
  * deployments (which set env vars directly) ignore the file even if it exists.
  */
object DotEnv:

  private lazy val fileEntries: Map[String, String] = load()

  def get(name: String): Option[String] =
    sys.env.get(name).orElse(fileEntries.get(name))

  private def load(): Map[String, String] =
    val file = new File(".env")
    if !file.exists() then Map.empty
    else
      val source = Source.fromFile(file)
      try
        source
          .getLines()
          .map(_.trim)
          .filter(line => line.nonEmpty && !line.startsWith("#"))
          .flatMap: line =>
            line.split("=", 2) match
              case Array(k, v) => Some(k.trim -> unquote(v.trim))
              case _           => None
          .toMap
      finally source.close()

  private def unquote(value: String): String =
    if value.length >= 2 && (
      (value.startsWith("\"") && value.endsWith("\"")) ||
      (value.startsWith("'")  && value.endsWith("'"))
    ) then value.substring(1, value.length - 1)
    else value

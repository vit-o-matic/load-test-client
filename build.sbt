import java.io.{FileInputStream, FileOutputStream, FileWriter}
import java.nio.file.{Files, Paths}

// Turn this project into a Scala.js project by importing these settings
enablePlugins(ScalaJSPlugin)

name := "load-test-client"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("utest.runner.Framework")

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.1",
  "com.lihaoyi" %%% "upickle" % "0.4.3",
  "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
)

def postCompile(result: Attributed[File]) = {
  val src = result.data
  val dest = new File("extension/background.js")
  println("Compiled to: " + result.data)
  new FileOutputStream(dest).getChannel.transferFrom(new FileInputStream(src).getChannel, 0, Long.MaxValue)
  val basePath = src.getAbsolutePath.substring(0, src.getAbsolutePath.lastIndexOf("-"))
  val launcher = Files.readAllBytes(Paths.get(new File(s"$basePath-launcher.js").toURI))
  val writer = new FileWriter(dest, true)
  writer.append(new String(launcher))
  writer.close()
  result
}

fastOptJS <<= (fastOptJS in Compile).map(postCompile(_))

fullOptJS <<= (fullOptJS in Compile).map(postCompile(_))


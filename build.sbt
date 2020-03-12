name := "mentoring-tagless-final"

version := "0.1"

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core"  % "2.1.1",
  "org.typelevel" %% "cats-effect"  % "2.1.2",
  "org.slf4j"      % "slf4j-simple" % "1.7.30",
  "org.scalatest" %% "scalatest"    % "3.1.1" % "test")
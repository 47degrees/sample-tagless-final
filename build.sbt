name := "mentoring-tagless-final"

version := "0.1"

scalaVersion := "2.12.7"

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect"  % "1.0.0",
  "org.slf4j"      % "slf4j-simple" % "1.7.25",
  "org.scalatest" %% "scalatest"    % "3.0.5" % "test")
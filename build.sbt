val Http4sVersion = "0.23.30"
val CirceVersion = "0.14.10"
val CatsEffectVersion = "3.5.7"
val MunitVersion = "1.0.3"
val MunitCEVersion = "2.0.0"

lazy val root = (project in file("."))
  .settings(
    name := "circuit-breaker-discovery",
    version := "0.1.0",
    scalaVersion := "3.3.4",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "org.http4s" %% "http4s-ember-client" % Http4sVersion,
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.scalameta" %% "munit" % MunitVersion % Test,
      "org.typelevel" %% "munit-cats-effect" % MunitCEVersion % Test,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
  )

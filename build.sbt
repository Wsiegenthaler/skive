name := "skive"

organization := "com.github.wsiegenthaler"

version := "0.8.2"

scalaVersion := "2.12.1"

crossScalaVersions := Seq("2.12.1", "2.11.8")

homepage := Some(url("http://github.com/wsiegenthaler/skive"))

libraryDependencies  ++= Seq(
  "org.scalanlp" %% "breeze" % "0.13",
  "org.scalanlp" %% "breeze-natives" % "0.13",
  "org.scalanlp" %% "breeze-viz" % "0.13")

resolvers ++= Seq(
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)


publishTo := {
  val nexus = "https://oss.sonatype.org"
  if (isSnapshot.value) Some("snapshots" at nexus + "/content/repositories/snapshots")
  else Some("releases" at nexus + "/service/local/staging/deploy/maven2")
}

licenses := Seq("BSD-style" -> url("https://opensource.org/licenses/BSD-3-Clause"))

pomExtra := (
  <scm>
    <connection>https://github.com/wsiegenthaler/skive.git</connection>
    <developerConnection>git@github.com:wsiegenthaler/skive.git</developerConnection>
    <url>https://github.com/wsiegenthaler/skive</url>
  </scm>
  <developers>
    <developer>
      <id>wsiegenthaler</id>
      <name>Weston Siegenthaler</name>
      <url>http://github.com/wsiegenthaler</url>
    </developer>
  </developers>)

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }


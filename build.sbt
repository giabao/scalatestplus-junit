name := "junit-4.12"

// need this because scalatest is not published for dotty 0.23 yet
val customBuild = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((0, n)) if n > 23 => true
    case _                      => false
  }
}
lazy val versionSuffix = Def.setting {
  val sv = scalaVersion.value
  val isDottyNightly = isDotty.value && sv.length > 10 // // "0.xx.0-bin".length
  if (isDottyNightly) "-dotty" + sv.substring(10)
  else ""
}

organization := (if (customBuild.value) "com.sandinh" else "org.scalatestplus")

version := "3.1.1.0" + versionSuffix.value

homepage := Some(url("https://github.com/scalatest/scalatestplus-junit"))

licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

developers := List(
  Developer(
    "bvenners",
    "Bill Venners",
    "bill@artima.com",
    url("https://github.com/bvenners")
  ),
  Developer(
    "cheeseng",
    "Chua Chee Seng",
    "cheeseng@amaseng.com",
    url("https://github.com/cheeseng")
  )
)

crossScalaVersions := List("2.10.7", "2.11.12", "2.12.11", "2.13.1", "0.22.0-RC1", "0.23.0-RC1", "0.24.0-bin-20200324-6cd3a9d-NIGHTLY")
scalaVersion := "0.23.0-RC1"

/** Add src/main/scala-{2|3} to Compile / unmanagedSourceDirectories */
Compile / unmanagedSourceDirectories +=
  (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((0 | 3, _)) => (Compile / sourceDirectory).value / "scala-3"
    case _                => (Compile / sourceDirectory).value / "scala-2"
  })

libraryDependencies ++= Seq(
  (if (customBuild.value) "com.sandinh"
   else "org.scalatest") %% "scalatest" % ("3.1.1" + versionSuffix.value),
  "junit" % "junit" % "4.13"
)
Test / scalacOptions ++= (if (isDotty.value) Seq("-language:implicitConversions") else Nil)

import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
import scala.xml.transform.{RewriteRule, RuleTransformer}

// skip dependency elements with a scope
pomPostProcess := { (node: XmlNode) =>
  new RuleTransformer(new RewriteRule {
    override def transform(node: XmlNode): XmlNodeSeq = node match {
      case e: Elem if e.label == "dependency"
          && e.child.exists(child => child.label == "scope") =>
        def txt(label: String): String = "\"" + e.child.filter(_.label == label).flatMap(_.text).mkString + "\""
        Comment(s""" scoped dependency ${txt("groupId")} % ${txt("artifactId")} % ${txt("version")} % ${txt("scope")} has been omitted """)
      case _ => node
    }
  }).transform(node).head
}

testOptions in Test :=
  Seq(
    Tests.Argument(TestFrameworks.ScalaTest,
      "-m", "org.scalatestplus.junit",
    ))

enablePlugins(SbtOsgi)

osgiSettings

OsgiKeys.exportPackage := Seq(
  "org.scalatestplus.junit.*"
)

OsgiKeys.importPackage := Seq(
  "org.scalatest.*",
  "org.scalactic.*", 
  "scala.*;version=\"$<range;[==,=+);$<replace;"+scalaBinaryVersion.value+";-;.>>\"",
  "*;resolution:=optional"
)

OsgiKeys.additionalHeaders:= Map(
  "Bundle-Name" -> "ScalaTestPlusJUnit",
  "Bundle-Description" -> "ScalaTest+JUnit is an open-source integration library between ScalaTest and JUnit for Scala projects.",
  "Bundle-DocURL" -> "http://www.scalatest.org/",
  "Bundle-Vendor" -> "Artima, Inc."
)

publishTo := sonatypePublishToBundle.value

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

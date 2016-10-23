/**
  * Copyright 2015 Groupon.com
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
import com.typesafe.sbt.pgp.PgpKeys._
import de.johoop.findbugs4sbt.FindBugs._
import de.johoop.findbugs4sbt.{Effort, Priority, ReportType}
import play.ebean.sbt.PlayEbean
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.PlayImport._
import play.sbt.PlayImport.PlayKeys._
import play.sbt.PlayJava
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys._

object ApplicationBuild extends Build {
  val appName = "artemis"
  name := appName

  val jacksonVersion = "2.8.4"
  val guiceVersion = "4.0"

  val s = findbugsSettings ++ CheckstyleSettings.checkstyleTask

  lazy val root = Project(appName, file("."), settings = s).enablePlugins(PlayJava, PlayEbean).settings(

  scalaVersion := "2.11.8",

  libraryDependencies ++= Seq(
    javaJdbc,
    javaWs,
    "cglib" % "cglib" % "3.2.4",
    "com.arpnetworking.build" % "build-resources" % "1.0.6",
    "com.arpnetworking.logback" % "logback-steno" % "1.16.1",
    "com.arpnetworking.metrics.extras" % "jvm-extra" % "0.4.2",
    "com.arpnetworking.metrics" % "metrics-client" % "0.5.0",
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion,
    "com.github.tomakehurst" % "wiremock" % "2.2.2" % "test",
    "com.google.code.findbugs" % "jsr305" % "3.0.1",
    "com.google.code.findbugs" % "annotations" % "3.0.1",
    "com.google.guava" % "guava" % "19.0",
    "com.google.inject" % "guice" % guiceVersion,
    "com.google.inject.extensions" % "guice-assistedinject" % guiceVersion,
    "com.h2database" % "h2" % "1.4.186",
    "com.hierynomus" % "sshj" % "0.18.0",
    "net.sf.oval" % "oval" % "1.86",
    "org.apache.httpcomponents" % "httpclient" % "4.3.1",
    "org.flywaydb" % "flyway-play_2.11" % "3.0.1",
    "org.mockito" % "mockito-all" % "1.10.19",
    "org.postgresql" % "postgresql" % "9.4-1202-jdbc42",
    "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0",
    "org.webjars" % "bootstrap" % "3.3.7",
    "org.webjars" % "jquery" % "3.1.1",
    "org.webjars" % "knockout" % "3.4.0",
    "org.webjars" % "typeaheadjs" % "0.11.1"
  ),

  // Extract build resources
  compile in Compile <<= (compile in Compile).dependsOn(Def.task {
    val jar = (update in Compile).value
      .select(configurationFilter("compile"))
      .filter(_.name.contains("build-resources"))
      .head
    IO.unzip(jar, (target in Compile).value / "build-resources")
    Seq.empty[File]
  }),

  // Compiler warnings as errors
  javacOptions ++= Seq(
    "-Xlint:all",
    "-Werror",
    "-Xlint:-path",
    "-Xlint:-try"
  ),

  // Findbugs
  findbugsReportType := Some(ReportType.Html),
  findbugsReportPath := Some(target.value / "findbugs" / "findbugs.html"),
  findbugsPriority := Priority.Low,
  findbugsEffort := Effort.Maximum,
  findbugsExcludeFilters := Some(
    <FindBugsFilter>
      <Match>
        <Class name="~views\.html\..*"/>
      </Match>
      <Match>
        <Class name="~models.*"/>
      </Match>
      <Match>
        <Class name="~router.Routes.*"/>
      </Match>
      <Match>
        <Class name="~_routes_.*"/>
      </Match>
      <Match>
        <Class name="~controllers\.routes.*"/>
      </Match>
      <Match>
        <Class name="~controllers\.Reverse.*"/>
      </Match>
      <Match>
        <Class name="~controllers\.config\.routes.*"/>
      </Match>
      <Match>
        <Class name="~controllers\.config\.Reverse.*"/>
      </Match>
      <Match>
        <Class name="~config\.Routes.*"/>
      </Match>
      <Match>
        <Class name="~artemis\.Routes.*"/>
      </Match>
      <Match>
        <Class name="~controllers\.impl\.proxy\.api\.routes.*"/>
      </Match>
      <Match>
        <Class name="~controllers\.impl\.proxy\.api\.Reverse.*"/>
      </Match>
    </FindBugsFilter>
  ),

  organization := "com.groupon",
  organizationName := "Groupon Inc",
  organizationHomepage := Some(new URL("https://github.com/Groupon")),

  publishMavenStyle := true,
  publishTo <<= version { v: String =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishTo in publishLocal := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
  pomIncludeRepository := { _ => false },
  pomExtra := (
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
        <comments>A business-friendly OSS license</comments>
      </license>
    </licenses>
      <url>https://github.com/Groupon/artemis</url>

      <developers>
        <developer>
          <id>barp</id>
          <name>Brandon Arp</name>
          <email>barp@groupon.com</email>
          <organization>Groupon</organization>
          <organizationUrl>http://www.groupon.com</organizationUrl>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>vkoskela</id>
          <name>Ville Koskela</name>
          <email>vkoskela@groupon.com</email>
          <organization>Groupon</organization>
          <organizationUrl>http://www.groupon.com</organizationUrl>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>dmisra</id>
          <name>Deepika Misra</name>
          <email>deepika@groupon.com</email>
          <organization>Groupon</organization>
          <organizationUrl>http://www.groupon.com</organizationUrl>
          <roles>
            <role>developer</role>
          </roles>
        </developer>
        <developer>
          <id>jviolette</id>
          <name>James Violette</name>
          <email>jviolette@groupon.com</email>
          <organization>Groupon</organization>
          <organizationUrl>http://www.groupon.com</organizationUrl>
        </developer>
        <developer>
          <id>kjungmeisteris</id>
          <name>Kevin Jungmeisteris</name>
          <email>kjungmeisteris@groupon.com</email>
          <organization>Groupon</organization>
          <organizationUrl>http://www.groupon.com</organizationUrl>
        </developer>
        <developer>
          <id>mhayter</id>
          <name>Matthew Hayter</name>
          <email>mhayter@groupon.com</email>
          <organization>Groupon</organization>
          <organizationUrl>http://www.groupon.com</organizationUrl>
        </developer>
        <developer>
          <id>ntimsina</id>
          <name>Nabin Timsina</name>
          <email>ntimsina@groupon.com</email>
          <organization>Groupon</organization>
          <organizationUrl>http://www.groupon.com</organizationUrl>
        </developer>
      </developers>

      <scm>
        <connection>scm:git:git@github.com:Groupon/artemis.git</connection>
        <developerConnection>scm:git:git@github.com:Groupon/artemis.git</developerConnection>
        <url>https://github.com/Groupon/artemis</url>
        <tag>HEAD</tag>
      </scm>
    ),

    releasePublishArtifactsAction := publishSigned.value,
    devSettings := Seq(("config.resource", "artemis-application.conf")),
    javaOptions in Test += "-Dconfig.file=conf/artemis-application.conf",
    routesGenerator := InjectedRoutesGenerator,

    scalaVersion := "2.11.6",
    resolvers += Resolver.mavenLocal,

    // Export assets artifact
    packagedArtifacts := {
      val artifacts: Map[sbt.Artifact, java.io.File] = (packagedArtifacts).value
      val assets: java.io.File = (playPackageAssets in Compile).value
      artifacts + (Artifact(moduleName.value, "jar", "jar", "assets") -> assets)
    },

    credentials += Credentials("Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    System.getenv("OSSRH_USER"),
    System.getenv("OSSRH_PASS")),

    useGpg := true,
    pgpPassphrase in Global := Option(System.getenv("GPG_PASS")).map(_.toCharArray),

    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),

    sonatypeProfileName := "com.groupon"
  )
}

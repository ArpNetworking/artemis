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
import play.sbt.PlayImport.PlayKeys._
import sbtrelease.ReleaseStateTransformations._
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}

name := "artemis"

val jacksonVersion = "2.10.5"

lazy val root = (project in file(".")).settings(SbtCheckstyle.checkstyleSettings).enablePlugins(PlayJava, PlayEbean, SbtCheckstyle, SbtPgp, SbtNativePackager, JavaServerAppPackaging, SystemVPlugin)

scalaVersion := "2.12.13"

libraryDependencies ++= Seq(
  javaJdbc,
  javaWs,
  guice,
  "cglib" % "cglib" % "3.3.0",
  "com.arpnetworking.build" % "build-resources" % "2.1.1",
  "com.arpnetworking.commons" % "commons" % "1.20.0",
  "com.arpnetworking.logback" % "logback-steno" % "1.18.5",
  "com.arpnetworking.metrics.extras" % "jvm-extra" % "0.11.2",
  "com.arpnetworking.metrics" % "metrics-client" % "0.11.3",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-guice" % jacksonVersion,
  "com.google.code.findbugs" % "jsr305" % "3.0.2",
  "com.google.code.findbugs" % "annotations" % "3.0.1",
  "com.google.guava" % "guava" % "30.1-jre",
  "com.h2database" % "h2" % "1.4.192",
  "com.hierynomus" % "sshj" % "0.23.0",
  "net.sf.oval" % "oval" % "1.86",
  "org.flywaydb" %% "flyway-play" % "7.2.0",
  "org.postgresql" % "postgresql" % "42.2.18",

  // Webjars
  "org.webjars" % "bootstrap" % "3.3.7",
  "org.webjars" % "jquery" % "3.1.1",
  "org.webjars" % "knockout" % "3.4.0",
  "org.webjars" %% "webjars-play" % "2.8.0-1",

  // Test dependencies
  "com.github.tomakehurst" % "wiremock-standalone" % "2.14.0" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"

)

// Extract build resources
compile in Compile := (compile in Compile).dependsOn(Def.task {
  val jar = (update in Compile).value
    .select(configurationFilter("compile"))
    .filter(_.name.contains("build-resources"))
    .head
  IO.unzip(jar, (target in Compile).value / "build-resources")
  Seq.empty[File]
}).value

javaOptions += "-Dconfig.file=conf/artemis.conf"

// Compiler warnings as errors
javacOptions ++= Seq(
  "-Xlint:all",
  "-Werror",
  "-Xlint:-path",
  "-Xlint:-try",
  // Needed because there is an annotation processor and the JUnit annotations
  // are not processed by it. See https://github.com/playframework/playframework/issues/1922#issuecomment-52884818
  // and https://bugs.openjdk.java.net/browse/JDK-6999068
  "-Xlint:-processing"
)

// Findbugs
findbugsReportType := Some(FindBugsReportType.Html)
findbugsReportPath := Some(target.value / "findbugs" / "findbugs.html")
findbugsPriority := FindBugsPriority.Low
findbugsEffort := FindBugsEffort.Maximum
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
)

organization := "com.arpnetworking"
organizationName := "ArpNetworking, Inc"
organizationHomepage := Some(new URL("https://github.com/ArpNetworking"))

javaOptions in Test += "-Dconfig.file=conf/artemis-application.conf"
javaOptions in Universal ++= Seq(s"-Dpidfile.path=/dev/null", s"-Dconfig.file=/etc/artemis/artemis.conf")

linuxPackageMappings += packageTemplateMapping(s"/usr/share/${name.value}/data")() withUser(name.value) withGroup(name.value)
linuxPackageMappings += packageTemplateMapping(s"/usr/share/${name.value}/data/h2")() withUser(name.value) withGroup(name.value)

publishMavenStyle := true
publishTo := version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}.value
publishTo in publishLocal := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
pomIncludeRepository := { _ => false }
pomExtra :=
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


releasePublishArtifactsAction := PgpKeys.publishSigned.value
devSettings := Seq(("config.resource", "artemis-dev.conf"))
javaOptions in Test += "-Dconfig.file=conf/application-base.conf"
routesGenerator := InjectedRoutesGenerator

//resolvers += Resolver.mavenLocal,

// Export assets artifact
packagedArtifacts := {
  val artifacts: Map[sbt.Artifact, java.io.File] = packagedArtifacts.value
  val assets: java.io.File = (playPackageAssets in Compile).value
  artifacts + (Artifact(moduleName.value, "jar", "jar", "assets") -> assets)
}

rpmVendor := "ArpNetworking"
rpmLicense := Option("ASL 2.0")
rpmUrl := Option("https://github.com/ArpNetworking/metrics-portal")
rpmRelease ~= { release =>
  if (release.equals("SNAPSHOT")) {
    val date = ZonedDateTime.now(ZoneId.of("Z"))
    val dateStamp = date.format(DateTimeFormatter.ofPattern("YYYYMMddHHmmss"))
    s"SNAPSHOT$dateStamp"
  } else {
    release
  }
}

credentials += Credentials("Sonatype Nexus Repository Manager",
"oss.sonatype.org",
System.getenv("OSSRH_USER"),
System.getenv("OSSRH_PASS"))

pgpPassphrase := Option(System.getenv("GPG_PASS")).map(_.toCharArray)
pgpSecretRing := file("./arpnetworking.key")

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
)

sonatypeProfileName := "com.arpnetworking"

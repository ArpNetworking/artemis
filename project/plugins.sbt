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
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "SBT Community repository" at "http://dl.bintray.com/sbt/sbt-plugin-releases/"

resolvers += "Typesafe repository plugin" at "https://repo.scala-sbt.org/scalasbt/sbt-plugin-releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" %% "sbt-plugin" % "2.5.9")

addSbtPlugin("com.typesafe.sbt" %% "sbt-play-ebean" % "3.1.0")

addSbtPlugin("de.johoop" %% "findbugs4sbt" % "1.4.0")

addSbtPlugin("com.typesafe.sbt" %% "sbt-digest" % "1.1.0")

addSbtPlugin("com.typesafe.sbt" %% "sbt-gzip" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.1.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.5.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

libraryDependencies ++= Seq(
  "com.puppycrawl.tools" % "checkstyle" % "6.3"
)

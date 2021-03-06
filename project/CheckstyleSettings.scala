/**
 * Copyright 2014 Groupon.com
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

import java.security.Permission

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

trait CheckstyleKeys {
  val checkstyle = TaskKey[Unit]("checkstyle", "run checkstyle, placing results in target/checkstyle")
}

object SbtCheckstyle extends AutoPlugin {

  object autoImport extends CheckstyleKeys

  override def requires = JvmPlugin
  override def trigger = allRequirements


  object CheckstyleFailedException extends Exception

  import autoImport._
  import Cs._

  lazy val checkstyleSettings: Seq[Setting[_]] = defaultCheckstyleSettings

  def defaultCheckstyleSettings = Seq(
    checkstyle := checkstyleTask.dependsOn(compile in Compile).value
  )

  object Cs {

    val checkstyleTask = Def.task {
          import com.puppycrawl.tools.checkstyle.Main.{main => CsMain}
          val outputDir = (target.value / "checkstyle").mkdirs
          val outputFile = (target.value / "checkstyle" / "checkstyle-report.xml").getAbsolutePath
          val inputDir = sourceDirectory.in(Compile).value.getAbsolutePath
          val buildDir = (target.value / "build-resources").getAbsoluteFile
          val args = List(
            "-c", (buildDir / "checkstyle.xml").getAbsolutePath,
            "-f", "xml",
            "-o", outputFile,
            inputDir
          )

          System.setProperty("header_file", (buildDir / "al2").toString)
          System.setProperty("suppressions_file", (baseDirectory.value / "checkstyle-suppressions.xml").toString)

          trappingExits {
            CsMain(args:_*)
          } match {
            case 0 =>
            case _ => throw CheckstyleFailedException
          }
    }
  }

  def trappingExits(thunk: => Unit): Int = {
    val originalSecManager = System.getSecurityManager
    case class NoExitsException(status: Int) extends SecurityException
    System setSecurityManager new SecurityManager() {


      override def checkPermission(perm: Permission): Unit = { }

      override def checkExit(status: Int): Unit = {
        super.checkExit(status)
        throw NoExitsException(status)
      }
    }
    try {
      thunk
      0
    } catch {
      case e:  NoExitsException => e.status
      case _ : Throwable => -1
    } finally {
      System setSecurityManager originalSecManager
    }
  }
}

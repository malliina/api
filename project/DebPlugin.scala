import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport.{Linux, daemonUser}
import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.Debian
import com.typesafe.sbt.packager.archetypes.systemloader.SystemdPlugin
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.SbtNativePackager.autoImport.{packageName, executableScriptName, packageSummary, maintainer, packageDescription}

object DebPlugin extends AutoPlugin {
  override def requires = JavaServerAppPackaging && SystemdPlugin

  object autoImport {
    val Deb = config("Deb")
  }
  import autoImport.Deb

  override def projectSettings: Seq[Setting[?]] = Seq(
    Linux / name := name.value,
    Linux / daemonUser := (Linux / name).value,
    Linux / packageName := (Linux / name).value,
    packageSummary := s"${(Linux / name).value} backend",
    packageDescription := s"${(Linux / name).value} backend.",
    executableScriptName := (Linux / name).value,
    Compile / packageDoc / mappings := Nil,
    Compile / packageDoc / publishArtifact := false,
    maintainer := "Michael Skogberg <malliina123@gmail.com>",
    Deb / packageBin := {
      val artifact = (Debian / packageBin).value
      val destName = (Linux / name).value
      val dest = target.value / s"$destName.deb"
      IO.copyFile(artifact, dest)
      streams.value.log.info(s"Copied '$artifact' to '$dest'.")
      dest
    },
    Deb / packageBin := (Deb / packageBin).dependsOn(Debian / packageBin).value
  )
}

import sbt.Keys.{name, publishArtifact, mappings, packageDoc}
import sbt.{Compile, Setting, AutoPlugin}
import com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport.{Linux, daemonUser}
import com.typesafe.sbt.packager.archetypes.systemloader.SystemdPlugin
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.SbtNativePackager.autoImport.{packageName, executableScriptName, packageSummary, maintainer, packageDescription}

object DebPlugin extends AutoPlugin {
  override def requires = JavaServerAppPackaging && SystemdPlugin

  override def projectSettings: Seq[Setting[?]] = Seq(
    Linux / name := name.value,
    Linux / daemonUser := (Linux / name).value,
    Linux / packageName := (Linux / name).value,
    packageSummary := s"${(Linux / name).value} backend",
    packageDescription := s"${(Linux / name).value} backend.",
    executableScriptName := (Linux / name).value,
    Compile / packageDoc / mappings := Nil,
    Compile / packageDoc / publishArtifact := false,
    maintainer := "Michael Skogberg <malliina123@gmail.com>"
  )
}

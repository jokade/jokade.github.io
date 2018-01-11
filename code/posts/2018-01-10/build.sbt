scalaVersion in ThisBuild := "2.11.12"

val Version = new {
  val obj_interop = "0.0.1-SNAPSHOT"
}


lazy val commonSettings = Seq(
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-language:implicitConversions","-Xlint"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies ++= Seq(
    "de.surfice" %%% "scalanative-obj-interop" % Version.obj_interop % "provided"
    ),
  nativeLinkingOptions ++= Seq("-lglib-2.0","-lgtk-3.0","-lgobject-2.0"),
  nativeLinkStubs := true
  )

lazy val manual = project
  .enablePlugins(ScalaNativePlugin)
  .settings(commonSettings:_*)

lazy val generated = project
  .enablePlugins(ScalaNativePlugin)
  .settings(commonSettings:_*)



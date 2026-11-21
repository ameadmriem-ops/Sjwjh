// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

tasks.register<Copy>("exportApkToHhh") {
  from(layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk"))
  into(layout.projectDirectory.dir("hhh"))
}

tasks.register<Zip>("exportProjectZip") {
  archiveFileName.set("MY_CINEMA_updated.zip")
  destinationDirectory.set(layout.projectDirectory)
  from(layout.projectDirectory) {
    exclude(
      ".gradle/**",
      ".kotlin/**",
      ".build-outputs/**",
      "build/**",
      "app/build/**",
      "hhh/**",
      "public/**",
      "*.zip",
      "debug.keystore",
      "debug.keystore.base64"
    )
  }
}

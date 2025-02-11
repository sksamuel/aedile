pluginManagement {
   repositories {
      gradlePluginPortal()
      mavenCentral()
   }
   plugins {
      kotlin("jvm").version("1.9.25")
   }
}

rootProject.name = "aedile"

include(
   "aedile-core",
   "aedile-micrometer",
)

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
   repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
   repositories {
      mavenCentral()
      mavenLocal()
      maven("https://oss.sonatype.org/content/repositories/snapshots/")
      maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
   }
   versionCatalogs {
      create("libs") {

         val coroutines = "1.8.1"
         library("coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
         library("coroutines-jdk8", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines")

         library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.1.8")
         library("micrometer-core", "io.micrometer:micrometer-core:1.13.2")

         val kotest = "5.9.1"
         library("kotest-datatest", "io.kotest:kotest-framework-datatest:$kotest")
         library("kotest-junit5", "io.kotest:kotest-runner-junit5:$kotest")
         library("kotest-core", "io.kotest:kotest-assertions-core:$kotest")
         library("kotest-json", "io.kotest:kotest-assertions-json:$kotest")
         library("kotest-property", "io.kotest:kotest-property:$kotest")

         bundle(
            "testing", listOf(
               "kotest-datatest",
               "kotest-junit5",
               "kotest-core",
               "kotest-json",
               "kotest-property",
            )
         )
      }
   }
}

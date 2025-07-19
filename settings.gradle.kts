pluginManagement {
   repositories {
      gradlePluginPortal()
      mavenCentral()
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

         library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.2")
         library("micrometer-core", "io.micrometer:micrometer-core:1.13.2")
      }
   }
}

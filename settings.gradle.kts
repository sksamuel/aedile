pluginManagement {
   repositories {
      gradlePluginPortal()
      mavenCentral()
   }
}

rootProject.name = "aedile"

include(
   "aedile-core",
)

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
   repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
   repositories {
      mavenCentral()
      mavenLocal()
   }
   versionCatalogs {
      create("libs") {

         val coroutines = "1.8.1"
         library("coroutines-core", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
         library("coroutines-jdk8", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines")

         library("caffeine", "com.github.ben-manes.caffeine:caffeine:3.2.2")
      }
   }
}

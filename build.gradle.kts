import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
   signing
   `maven-publish`
   kotlin("jvm")
}

subprojects {
   apply(plugin = "org.jetbrains.kotlin.jvm")

   group = "com.sksamuel.aedile"
   version = Ci.version

   dependencies {
      api(rootProject.libs.coroutines.core)
      api(rootProject.libs.coroutines.jdk8)
      testApi(rootProject.libs.bundles.testing)
   }

   tasks.named<Test>("test") {
      useJUnitPlatform()
      testLogging {
         showExceptions = true
         showStandardStreams = true
         exceptionFormat = TestExceptionFormat.FULL
      }
   }

   java {
      toolchain {
         languageVersion.set(JavaLanguageVersion.of(11))
      }
      withSourcesJar()
   }

   tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
      kotlinOptions.jvmTarget = "11"
      kotlinOptions.apiVersion = "1.9"
      kotlinOptions.languageVersion = "1.9"
   }
}

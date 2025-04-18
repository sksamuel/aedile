import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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
      compilerOptions {
         jvmTarget = JvmTarget.JVM_11
         apiVersion = KotlinVersion.KOTLIN_1_9
         languageVersion = KotlinVersion.KOTLIN_1_9
      }
   }
}

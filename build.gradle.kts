import org.gradle.api.tasks.testing.logging.TestExceptionFormat

buildscript {
   repositories {
      mavenCentral()
      maven {
         url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
      }
      maven {
         url = uri("https://plugins.gradle.org/m2/")
      }
   }
}

plugins {
   signing
   `maven-publish`
   kotlin("jvm").version("1.7.22")
}

allprojects {
   apply(plugin = "org.jetbrains.kotlin.jvm")

   group = "com.sksamuel.aedile"
   version = Ci.version

   repositories {
      mavenLocal()
      mavenCentral()
      maven {
         url = uri("https://oss.sonatype.org/content/repositories/snapshots")
      }
   }

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
   }

   tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
      kotlinOptions.jvmTarget = "11"
   }
}

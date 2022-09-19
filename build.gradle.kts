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
   kotlin("jvm").version("1.7.10")
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
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
      implementation("com.sksamuel.tabby:tabby-fp:2.0.34")
      api("com.github.ben-manes.caffeine:caffeine:3.1.1")

      testImplementation("io.kotest:kotest-framework-datatest:5.4.2")
      testImplementation("io.kotest:kotest-runner-junit5:5.4.2")
      testImplementation("io.kotest:kotest-assertions-core:5.4.2")
      testImplementation("io.kotest:kotest-property:5.4.2")
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

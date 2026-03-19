import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
   `java-library`
   kotlin("jvm")
   id("io.kotest")
}

java {
   toolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
   }
   sourceCompatibility = JavaVersion.VERSION_11
   targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
   jvmToolchain(11)
   compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
      apiVersion.set(KotlinVersion.KOTLIN_2_2)
      languageVersion.set(KotlinVersion.KOTLIN_2_2)
   }
}

tasks.compileJava {
   options.release = 11
}

tasks.compileTestJava {
   options.release = 11
}

tasks.compileTestKotlin {
   compilerOptions.jvmTarget = JvmTarget.JVM_11
}

dependencies {
   testImplementation(kotlin("stdlib"))

   val kotest = "6.1.7"
   testImplementation("io.kotest:kotest-runner-junit5:$kotest")
   testImplementation("io.kotest:kotest-assertions-core:$kotest")
   testImplementation("io.kotest:kotest-assertions-json:$kotest")
   testImplementation("io.kotest:kotest-property:$kotest")
}

tasks.named<Test>("test") {
   useJUnitPlatform()
   filter {
      isFailOnNoMatchingTests = false
   }
   testLogging {
      showExceptions = true
      showStandardStreams = true
      events = setOf(
         TestLogEvent.FAILED,
         TestLogEvent.PASSED
      )
      exceptionFormat = TestExceptionFormat.FULL
   }
}

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
   `java-library`
   kotlin("jvm")
}

java {
   toolchain {
      languageVersion.set(JavaLanguageVersion.of(11))
   }
   sourceCompatibility = JavaVersion.VERSION_11
   targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
   compilerOptions {
      jvmTarget.set(JvmTarget.JVM_11)
      apiVersion.set(KotlinVersion.KOTLIN_1_9)
      languageVersion.set(KotlinVersion.KOTLIN_1_9)
   }
}

dependencies {
   testImplementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.21")

   val kotest = "6.1.4"
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

import org.gradle.kotlin.dsl.`kotlin-dsl`

repositories {
   mavenCentral()
   gradlePluginPortal()
}

plugins {
   `kotlin-dsl`
}

dependencies {
   implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")
   implementation("io.kotest:io.kotest.gradle.plugin:6.1.11")
   implementation("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:0.35.0")
}

plugins {
   `java-test-fixtures`
}

dependencies {
   api(libs.caffeine)
}

apply("../publish.gradle.kts")

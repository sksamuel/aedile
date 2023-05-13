dependencies {
   api(projects.aedileCore)
   api(libs.micrometer.core)
}

apply("../publish.gradle.kts")

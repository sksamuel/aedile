dependencies {
   api(project(":aedile-core"))
   api("io.micrometer:micrometer-core:1.9.4")
}

apply("../publish.gradle.kts")

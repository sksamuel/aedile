dependencies {
   api(project(":aedile-core"))
   api("io.micrometer:micrometer-core:_")
}

apply("../publish.gradle.kts")

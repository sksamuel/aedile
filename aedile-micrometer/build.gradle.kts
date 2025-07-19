plugins {
   id("kotlin-conventions")
   id("publishing-conventions")
}

dependencies {
   api(projects.aedileCore)
   api(libs.micrometer.core)
}

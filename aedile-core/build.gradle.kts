plugins {
   id("kotlin-conventions")
   id("publishing-conventions")
}

dependencies {
   api(libs.caffeine)
   api(rootProject.libs.coroutines.core)
   api(rootProject.libs.coroutines.jdk8)
}

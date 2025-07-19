plugins {
   id("com.vanniktech.maven.publish")
}

group = "com.sksamuel.aedile"
version = Ci.version

mavenPublishing {
   publishToMavenCentral(automaticRelease = true)
   signAllPublications()
   pom {
      name.set("aedile")
      description.set("Kotlin Wrapper for Caffeine")
      url.set("http://www.github.com/sksamuel/aedile")

      scm {
         connection.set("scm:git:http://www.github.com/sksamuel/aedile/")
         developerConnection.set("scm:git:http://github.com/sksamuel/")
         url.set("http://www.github.com/sksamuel/aedile/")
      }

      licenses {
         license {
            name.set("The Apache 2.0 License")
            url.set("https://opensource.org/licenses/Apache-2.0")
         }
      }

      developers {
         developer {
            id.set("sksamuel")
            name.set("Stephen Samuel")
            email.set("sam@sksamuel.com")
         }
      }
   }
}

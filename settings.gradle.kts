rootProject.name = "aedile"

plugins {
   id("de.fayard.refreshVersions") version "0.51.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

refreshVersions {
   enableBuildSrcLibs()
}

refreshVersions {
   enableBuildSrcLibs()
   rejectVersionIf {
      candidate.stabilityLevel != de.fayard.refreshVersions.core.StabilityLevel.Stable
   }
}

include(
   "aedile-core",
   "aedile-micrometer",
)

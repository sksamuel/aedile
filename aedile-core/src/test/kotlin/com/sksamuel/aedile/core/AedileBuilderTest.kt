package com.sksamuel.aedile.core

import io.kotest.core.spec.style.FunSpec
import kotlin.time.Duration.Companion.seconds

class AedileBuilderTest : FunSpec() {
   init {
      test("cache with configuration options") {
         caffeineBuilder<String, String> {
            maximumWeight = 100
            weigher = { _, _ -> 1 }
            expireAfterAccess = 10.seconds
         }.build()
      }
   }
}

package com.sksamuel.aedile.core

import io.kotest.core.spec.style.FunSpec

class AedileBuilderTest : FunSpec() {
   init {
      test("cache with configuration options") {
         val cache = caffeineBuilder()
            .maximumSize(100)
            .initialCapacity(10)
            .build<String, String>()
      }
   }
}

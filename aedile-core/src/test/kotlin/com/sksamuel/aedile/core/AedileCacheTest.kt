package com.sksamuel.aedile.core

import io.kotest.core.spec.style.FunSpec

class AedileCacheTest : FunSpec() {
   init {
      test("cache.get should suspend") {
         val cache: Aedile<String, String> = caffeineBuilder().build<String, String>()
         cache.get("foo")
      }
   }
}

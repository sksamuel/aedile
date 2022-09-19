package com.sksamuel.aedile.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class CacheTest : FunSpec() {
   init {

      test("Cache should return null for missing keys") {
         val cache = caffeineBuilder<String, String>().build()
         cache.getIfPresent("else") shouldBe null
      }

      test("Cache should support suspendable compute function") {
         val cache = caffeineBuilder<String, String>().build()
         cache.getOrPut("foo") {
            delay(1)
            "bar"
         } shouldBe "bar"
      }
   }
}

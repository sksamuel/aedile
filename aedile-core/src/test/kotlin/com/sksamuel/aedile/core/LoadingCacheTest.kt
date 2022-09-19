package com.sksamuel.aedile.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class LoadingCacheTest : FunSpec() {
   init {

      test("LoadingCache should use support suspendable loading function") {
         val cache = caffeineBuilder().build<String, String> {
            delay(1)
            "bar"
         }
         cache.get("else") shouldBe "bar"
      }

      test("LoadingCache should support suspendable put") {
         val cache = caffeineBuilder().build<String, String> {
            delay(1)
            "bar"
         }
         cache.put("foo") {
            delay(1)
            "wibble"
         }
         cache.get("foo") shouldBe "wibble"
         cache.get("else") shouldBe "bar"
      }
   }
}

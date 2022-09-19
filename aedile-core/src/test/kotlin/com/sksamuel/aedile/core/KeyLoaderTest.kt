package com.sksamuel.aedile.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class KeyLoaderTest : FunSpec() {
   init {

      test("cache should use custom key loader") {
         val cache = caffeineBuilder<String, String>().build()
         cache.getOrPut("foo") {
            "bar"
         } shouldBe "bar"
      }

      test("cache should use support suspendable functions in the key loader") {
         val cache = caffeineBuilder<String, String>().build()
         cache.getOrPut("foo") {
            delay(1)
            "bar"
         } shouldBe "bar"
      }
   }
}

package com.sksamuel.aedile.core

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class CacheTest : FunSpec() {
   init {

      test("Cache should return null for missing keys") {
         val cache = caffeineBuilder<String, String>().build()
         cache.getIfPresent("else") shouldBe null
      }

      test("Cache should support simple puts") {
         val cache = caffeineBuilder<String, String>().build()
         cache["foo"] = "bar"
      }

      test("Cache.get should support suspendable compute function") {
         val cache = caffeineBuilder<String, String>().build()
         cache.get("foo") {
            delay(1)
            "bar"
         } shouldBe "bar"
      }

      test("cache should handle exceptions in the compute function") {
         val cache = caffeineBuilder<String, String>().build()
         shouldThrowAny {
            cache.get("foo") {
               error("kapow")
            }
         }
         cache.get("bar") {
            "baz"
         } shouldBe "baz"
      }

      test("Cache should support asDeferredMap") {
         val cache = caffeineBuilder<String, String>().build()
         cache.put("wibble", "wobble")
         cache.put("bubble", "bobble")
         val map = cache.asDeferredMap()
         map["wibble"]?.await() shouldBe "wobble"
         map["bubble"]?.await() shouldBe "bobble"
      }

      test("Cache should support getAll") {
         val cache = caffeineBuilder<String, String>().build()
         cache.put("foo") {
            delay(1)
            "wobble"
         }
         cache.put("bar") {
            delay(1)
            "wibble"
         }
         cache.put("baz") {
            delay(1)
            "wubble"
         }
         cache.getAll(listOf("foo", "bar", "baz")) {
            mapOf("baz" to "wubble")
         } shouldBe mapOf("foo" to "wobble", "bar" to "wibble", "baz" to "wubble")
      }


      test("Cache should support asMap") {
         val cache = caffeineBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.put("foo") {
            delay(1)
            "wobble"
         }
         cache.put("bar") {
            delay(1)
            "wibble"
         }
         cache.asMap() shouldBe mapOf("foo" to "wobble", "bar" to "wibble")
      }
   }
}

package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class AsCacheTest : FunSpec() {
   init {

      test("should support simple puts") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         cache.put("foo", "bar")
         cache["baz"] = "waz"
         cache.getIfPresent("foo") shouldBe "bar"
         cache.getIfPresent("baz") shouldBe "waz"
      }

      test("should support getIfPresent") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         cache.put("foo", "bar")
         cache.getIfPresent("foo") shouldBe "bar"
         cache.getIfPresent("baqwewqewqz") shouldBe null
      }

      test("get should support suspendable compute function") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         cache.get("foo") {
            delay(1)
            "bar"
         } shouldBe "bar"
      }

      test("get should propagate exceptions in the compute function") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         shouldThrow<IllegalStateException> {
            cache.get("foo") {
               error("kapow")
            }
         }
      }

      test("getAll should propagate exceptions in the compute function") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         shouldThrow<IllegalStateException> {
            cache.getAll(setOf("foo", "bar")) {
               error("kapow")
            }
         }
      }

      test("should support getAll") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
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

      test("should support asMap") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         cache.put("wibble", "wobble")
         cache["bubble"] = "bobble"
         cache.asMap() shouldBe mapOf("wibble" to "wobble", "bubble" to "bobble")
      }

      test("should support asDeferredMap") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         cache.put("wibble", "wobble")
         cache["bubble"] = "bobble"
         val map = cache.asDeferredMap()
         map["wibble"]?.await() shouldBe "wobble"
         map["bubble"]?.await() shouldBe "bobble"
      }

      test("should support invalidate") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         cache.put("wibble", "wobble")
         cache.getIfPresent("wibble") shouldBe "wobble"
         cache.invalidate("wibble")
         cache.getIfPresent("wibble") shouldBe null
      }

      test("should support contains") {
         val cache = Caffeine.newBuilder().asCache<String, String>()
         cache.put("wibble", "wobble")
         cache.contains("wibble") shouldBe true
         cache.contains("bubble") shouldBe false
      }
   }
}

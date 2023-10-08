package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class LoadingCacheTest : FunSpec() {
   init {

      test("LoadingCache should use support suspendable loading function") {
         val cache = caffeineBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.get("else") shouldBe "bar"
      }

      test("LoadingCache should use support suspendable multiple keys loading function") {
         val cache = caffeineBuilder<String, String>().buildAll {
            delay(1)
            mapOf("tweedle" to "dee", "twuddle" to "dum")
         }
         cache.get("tweedle") shouldBe "dee"
         cache.get("twuddle") shouldBe "dum"
      }

      test("LoadingCache should support simple puts") {
         val cache = caffeineBuilder<String, String>().build()
         cache.put("foo", "bar")
         cache["baz"] = "waz"
         cache.getIfPresent("foo") shouldBe "bar"
         cache.getIfPresent("baz") shouldBe "waz"
      }

      test("LoadingCache should support getOrPut") {

         val cache = caffeineBuilder<String, String>().build {
            delay(1)
            "bar"
         }

         cache.get("foo") {
            delay(1)
            "wibble"
         } shouldBe "wibble"

         cache.get("foo") {
            delay(1)
            "wobble"
         } shouldBe "wibble"
      }

      test("foo") {
         val cache = Caffeine.newBuilder().build<String, String> {
            error("kaboom")
         }
         cache.get("foo") { "baz" } shouldBe "baz"
         cache.get("bar") { "baz" } shouldBe "baz"
      }

      test("get should throw if build compute function throws and key is missing") {
         val cache = caffeineBuilder<String, String>().build {
            error("kaput")
         }
         shouldThrow<IllegalStateException> {
            cache.get("foo")
         }
      }

      test("getIfPresent should return null if build compute function throws and key is missing") {
         val cache = caffeineBuilder<String, String>().build {
            error("kaput")
         }
         cache.getIfPresent("foo") shouldBe null
      }

      test("get should use existing value if build compute function throws and key is present") {
         val cache = caffeineBuilder<String, String>().build {
            error("kaput")
         }
         cache.get("foo") { "bar" } shouldBe "bar"
         cache.get("foo") shouldBe "bar"
      }

      test("get should propagate exceptions if the override throws") {
         val cache = caffeineBuilder<String, String>().buildAll { keys ->
            keys.associateWith { "$it-value" }
         }
         shouldThrow<IllegalStateException> {
            cache.get("foo") { error("kapow") }
         }
         cache.get("bar") { "baz" } shouldBe "baz"
      }

      test("getAll should throw if build compute function throws and any key is missing") {
         val cache = caffeineBuilder<String, String>().build {
            error("kaput")
         }
         shouldThrow<IllegalStateException> {
            cache.getAll(setOf("foo", "bar"))
         }
         delay(100)
         cache.get("foo") { "baz" } shouldBe "baz"
         delay(100)
         shouldThrow<IllegalStateException> {
            cache.getAll(setOf("bar"))
         }
         delay(100)
         cache.getAll(setOf("foo")) shouldBe mapOf("foo" to "baz")
      }

      test("getAll should throw if build compute function override throws and any key is missing") {
         val cache = caffeineBuilder<String, String>().build {
            "$it-value"
         }
         shouldThrow<IllegalStateException> {
            cache.getAll(setOf("foo", "bar")) { error("boom") }
         }
         cache.getAll(setOf("foo")) shouldBe mapOf("foo" to "foo-value")
         shouldThrow<IllegalStateException> {
            cache.getAll(setOf("foo", "bar")) { error("boom") }
         }
      }

      test("getAll should return existing values if the build function throws but values are present") {
         val cache = caffeineBuilder<String, String>().build {
            error("kaput")
         }
         cache.get("foo") { "baz" } shouldBe "baz"
         cache.get("bar") { "faz" } shouldBe "faz"
         cache.getAll(setOf("foo", "bar")) shouldBe mapOf("foo" to "baz", "bar" to "faz")
      }

      test("LoadingCache should propagate exceptions in the build compute function override") {
         val cache = caffeineBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         shouldThrowAny {
            cache.get("foo") {
               error("kapow")
            }
         }
         cache.get("bar") { "baz" } shouldBe "baz"
      }

      test("LoadingCache should propagate exceptions in the buildAll compute function override") {
         val cache = caffeineBuilder<String, String>().buildAll { keys ->
            keys.associateWith { "$it-value" }
         }
         shouldThrowAny {
            cache.get("foo") {
               error("kapow")
            }
         }
         cache.get("bar") {
            "baz"
         } shouldBe "baz"
      }

      test("LoadingCache should support suspendable put") {
         val cache = caffeineBuilder<String, String>().build {
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

      test("LoadingCache should support getAll") {
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
         cache.put("baz") {
            delay(1)
            "wubble"
         }
         cache.getAll(listOf("foo", "bar")) shouldBe mapOf("foo" to "wobble", "bar" to "wibble")
      }

      test("getAll should support suspendable multiple keys loading function override") {
         val cache = caffeineBuilder<String, String>().buildAll {
            delay(1)
            mapOf("tweedle" to "dee", "twuddle" to "dum")
         }
         cache.get("tweedle") shouldBe "dee"
         cache.get("twuddle") shouldBe "dum"
         cache.getAll(setOf("wibble", "wobble")) {
            it.associateWith { "$it-value" }
         } shouldBe mapOf(
            "wibble" to "wibble-value",
            "wobble" to "wobble-value"
         )
      }

      test("LoadingCache should support refreshAfterWrite using refresh compute function") {
         val cache = caffeineBuilder<String, Int> {
            refreshAfterWrite = 10.milliseconds
         }.build({ 0 }, { _, old -> old + 1 })
         cache.get("foo") shouldBe 0
         delay(100)
         cache.get("foo") shouldBe 1
         delay(100)
         cache.get("foo") shouldBe 2
      }

      test("LoadingCache should support asMap") {
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

      test("LoadingCache should support asDeferredMap") {
         val cache = caffeineBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.put("wibble") {
            delay(1)
            "wobble"
         }
         cache.put("bubble") {
            delay(1)
            "bobble"
         }
         val map = cache.asDeferredMap()
         map["wibble"]?.await() shouldBe "wobble"
         map["bubble"]?.await() shouldBe "bobble"
      }

      test("LoadingCache.getIfPresent") {
         val cache = caffeineBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.getIfPresent("foo") shouldBe null
         cache.put("foo") { "baz" }
         cache.getIfPresent("foo") shouldBe "baz"
      }

      test("Cache should support invalidate") {
         val cache: LoadingCache<String, String> = caffeineBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.put("wibble", "wobble")
         cache.getIfPresent("wibble") shouldBe "wobble"
         cache.invalidate("wibble")
         cache.getIfPresent("wibble") shouldBe null
      }

      test("Cache should support contains") {
         val cache: LoadingCache<String, String> = caffeineBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.put("wibble", "wobble")
         cache.contains("wibble") shouldBe true
         cache.contains("bubble") shouldBe false
      }
   }
}

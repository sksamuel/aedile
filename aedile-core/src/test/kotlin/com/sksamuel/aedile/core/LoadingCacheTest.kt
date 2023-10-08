package com.sksamuel.aedile.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class LoadingCacheTest : FunSpec() {
   init {

      test("LoadingCache should use support suspendable loading function") {
         val cache = cacheBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.get("else") shouldBe "bar"
      }

      test("LoadingCache should use support suspendable multiple keys loading function") {
         val cache = cacheBuilder<String, String>().buildAll {
            delay(1)
            mapOf("tweedle" to "dee", "twuddle" to "dum")
         }
         cache.get("tweedle") shouldBe "dee"
         cache.get("twuddle") shouldBe "dum"
      }

      test("LoadingCache should support simple puts") {
         val cache = cacheBuilder<String, String>().build()
         cache.put("foo", "bar")
         cache["baz"] = "waz"
         cache.getIfPresent("foo") shouldBe "bar"
         cache.getIfPresent("baz") shouldBe "waz"
      }

      test("LoadingCache should support getOrPut") {

         val cache = cacheBuilder<String, String>().build {
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

      test("get should throw if build compute function throws and key is missing") {
         val cache = cacheBuilder<String, String>().build {
            error("kaput")
         }
         shouldThrow<IllegalStateException> {
            cache.get("foo")
         }
      }

      test("getIfPresent should return null if build compute function throws and key is missing") {
         val cache = cacheBuilder<String, String>().build {
            error("kaput")
         }
         cache.getIfPresent("foo") shouldBe null
      }

      test("get should use existing value if build compute function throws and key is present") {
         val cache = cacheBuilder<String, String>().build {
            error("kaput")
         }
         cache.get("foo") { "bar" } shouldBe "bar"
         cache.get("foo") shouldBe "bar"
      }

      test("get should propagate exceptions if the override throws") {
         val cache = cacheBuilder<String, String>().buildAll { keys ->
            keys.associateWith { "$it-value" }
         }
         shouldThrow<IllegalStateException> {
            supervisorScope {
               cache.get("foo") { error("kapow") }
            }
         }
      }

      test("getAll should throw if build compute function throws and any key is missing") {
         val cache = cacheBuilder<String, String>().build {
            error("kaput")
         }
         shouldThrow<IllegalStateException> {
            supervisorScope {
               cache.getAll(setOf("foo", "bar"))
            }
         }
         cache.get("foo") { "baz" } shouldBe "baz"
         shouldThrow<IllegalStateException> {
            supervisorScope {
               cache.getAll(setOf("bar"))
            }
         }
         cache.getAll(setOf("foo")) shouldBe mapOf("foo" to "baz")
      }

      test("getAll should throw if build compute function override throws and any key is missing") {
         val cache = cacheBuilder<String, String>().build {
            "$it-value"
         }
         shouldThrow<IllegalStateException> {
            supervisorScope {
               cache.getAll(setOf("foo", "bar")) { error("boom") }
            }
         }
         cache.getAll(setOf("foo")) shouldBe mapOf("foo" to "foo-value")
         shouldThrow<IllegalStateException> {
            supervisorScope {
               cache.getAll(setOf("foo", "bar")) { error("boom") }
            }
         }
      }

      test("getAll should return existing values if the build function throws but values are present") {
         val cache = cacheBuilder<String, String>().build {
            error("kaput")
         }
         cache.get("foo") { "baz" } shouldBe "baz"
         cache.get("bar") { "faz" } shouldBe "faz"
         cache.getAll(setOf("foo", "bar")) shouldBe mapOf("foo" to "baz", "bar" to "faz")
      }

      test("LoadingCache should propagate exceptions in the build compute function override") {
         val cache = cacheBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         shouldThrow<IllegalStateException> {
            supervisorScope {
               cache.get("foo") {
                  error("kapow")
               }
            }
         }
         cache.get("bar") { "baz" } shouldBe "baz"
      }

      test("LoadingCache should propagate exceptions in the buildAll compute function override") {
         val cache = cacheBuilder<String, String>().buildAll { keys ->
            keys.associateWith { "$it-value" }
         }
         shouldThrow<IllegalStateException> {
            supervisorScope {
               cache.get("foo") {
                  error("kapow")
               }
            }
         }
         cache.get("bar") { "baz" } shouldBe "baz"
      }

      test("LoadingCache should support suspendable put") {
         val cache = cacheBuilder<String, String>().build {
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
         val cache = cacheBuilder<String, String>().build {
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
         val cache = cacheBuilder<String, String>().buildAll {
            delay(1)
            mapOf("tweedle" to "dee", "twuddle" to "dum")
         }
         cache.get("tweedle") shouldBe "dee"
         cache.get("twuddle") shouldBe "dum"
         cache.getAll(setOf("wibble", "wobble")) { keys ->
            keys.associateWith { "$it-value" }
         } shouldBe mapOf(
            "wibble" to "wibble-value",
            "wobble" to "wobble-value"
         )
      }

      test("LoadingCache should support refreshAfterWrite using refresh compute function") {
         val value = AtomicInteger(0)
         val cache = cacheBuilder<String, Int> {
            refreshAfterWrite = 10.milliseconds
         }.build({ value.get() }, { _, _ ->
            value.incrementAndGet()
         })
         cache.get("foo")
         cache.get("foo")
         value.get() shouldBe 0
         delay(100)
         cache.get("foo")
         delay(100)
         value.get() shouldBe 1
         delay(100)
         cache.get("foo")
         delay(100)
         value.get() shouldBe 2
      }

      test("LoadingCache should support asMap") {
         val cache = cacheBuilder<String, String>().build {
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
         val cache = cacheBuilder<String, String>().build {
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
         val cache = cacheBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.getIfPresent("foo") shouldBe null
         cache.put("foo") { "baz" }
         cache.getIfPresent("foo") shouldBe "baz"
      }

      test("Cache should support invalidate") {
         val cache: LoadingCache<String, String> = cacheBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.put("wibble", "wobble")
         cache.getIfPresent("wibble") shouldBe "wobble"
         cache.invalidate("wibble")
         cache.getIfPresent("wibble") shouldBe null
      }

      test("Cache should support contains") {
         val cache: LoadingCache<String, String> = cacheBuilder<String, String>().build {
            delay(1)
            "bar"
         }
         cache.put("wibble", "wobble")
         cache.contains("wibble") shouldBe true
         cache.contains("bubble") shouldBe false
      }
   }
}

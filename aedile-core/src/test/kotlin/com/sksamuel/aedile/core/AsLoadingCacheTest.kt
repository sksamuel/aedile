package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AsLoadingCacheTest : FunSpec() {
   init {

      test("should support suspendable loading function") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            delay(1)
            "bar"
         }
         cache.get("else") shouldBe "bar"
      }

      test("should support suspendable bulk loading function") {
         val cache = Caffeine.newBuilder().asBulkLoadingCache {
            delay(1)
            mapOf("tweedle" to "dee", "twuddle" to "dum")
         }
         cache.get("tweedle") shouldBe "dee"
         cache.get("twuddle") shouldBe "dum"
      }

      test("LoadingCache should support simple puts") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            "bar"
         }
         cache.put("foo", "bar")
         cache["baz"] = "waz"
         cache.getIfPresent("foo") shouldBe "bar"
         cache.getIfPresent("baz") shouldBe "waz"
      }

      test("LoadingCache should support getOrPut") {

         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
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
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            error("kaput")
         }
         shouldThrow<IllegalStateException> {
            cache.get("foo")
         }
      }

      test("Cache should support getOrNull with compute") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            "boo"
         }
         cache.getOrNull("foo") {
            yield()
            "bar"
         } shouldBe "bar"
         val key = "baz"
         cache.getOrNull(key) { "new value" }.shouldBe("new value")
         cache.getOrNull(key) { "new value 2" }.shouldBe("new value")
         cache.getOrNull(key) { null }.shouldBe("new value")
         shouldNotThrowAny { cache.getOrNull(key) { error("kapow") } }
      }

      test("getIfPresent should return null if build compute function throws and key is missing") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            error("kaput")
         }
         cache.getIfPresent("foo") shouldBe null
      }

      test("get should use existing value if build compute function throws and key is present") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            error("kaput")
         }
         cache.get("foo") { "bar" } shouldBe "bar"
         cache.get("foo") shouldBe "bar"
      }

      test("get should propagate exceptions if the override throws") {
         val cache = Caffeine.newBuilder().asBulkLoadingCache<String, String> { keys ->
            keys.associateWith { "$it-value" }
         }
         shouldThrow<IllegalStateException> {
            supervisorScope {
               cache.get("foo") { error("kapow") }
            }
         }
      }

      test("getAll should throw if build compute function throws and any key is missing") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
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
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
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
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            error("kaput")
         }
         cache.get("foo") { "baz" } shouldBe "baz"
         cache.get("bar") { "faz" } shouldBe "faz"
         cache.getAll(setOf("foo", "bar")) shouldBe mapOf("foo" to "baz", "bar" to "faz")
      }

      test("LoadingCache should propagate exceptions in the build compute function override") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
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
         val cache = Caffeine.newBuilder().asBulkLoadingCache<String, String> { keys ->
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
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
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

      test("should support getAll") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
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
         val cache = Caffeine.newBuilder().asBulkLoadingCache {
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

      test("should support refreshAfterWrite using compute function") {
         val value = AtomicInteger(0)
         val cache = Caffeine.newBuilder().refreshAfterWrite(25.milliseconds).asLoadingCache<String, Int> {
            value.incrementAndGet()
         }
         cache.get("foo")
         cache.get("foo")
         cache.get("foo")
         cache.get("foo")
         value.get() shouldBe 1
         delay(100)
         cache.get("foo")
         delay(100)
         value.get() shouldBe 2
         delay(100)
         cache.get("foo")
         delay(100)
         value.get() shouldBe 3
      }

      test("should support refreshAfterWrite using recompute function") {
         val value = AtomicInteger(0)
         val cache = Caffeine.newBuilder().refreshAfterWrite(25.milliseconds).asLoadingCache<String, Int>(
            compute = { value.incrementAndGet() },
            reloadCompute = { key, old ->
               value.incrementAndGet()
               value.incrementAndGet()
               value.incrementAndGet()
            }
         )
         cache.get("foo")
         cache.get("foo")
         cache.get("foo")
         cache.get("foo")
         value.get() shouldBe 1
         delay(100)
         cache.get("foo")
         delay(100)
         value.get() shouldBe 4
         delay(100)
         cache.get("foo")
         delay(100)
         value.get() shouldBe 7
      }

      test("should support asMap") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
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
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
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
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            delay(1)
            "bar"
         }
         cache.getIfPresent("foo") shouldBe null
         cache.put("foo") { "baz" }
         cache.getIfPresent("foo") shouldBe "baz"
      }

      test("Cache should support invalidate") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            delay(1)
            "bar"
         }
         cache.put("wibble", "wobble")
         cache.getIfPresent("wibble") shouldBe "wobble"
         cache.invalidate("wibble")
         cache.getIfPresent("wibble") shouldBe null
      }

      test("Cache should support invalidateAll") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            delay(1)
            "bar"
         }
         cache.put("wibble", "a")
         cache.put("wobble", "b")
         cache.getIfPresent("wibble") shouldBe "a"
         cache.getIfPresent("wobble") shouldBe "b"
         cache.invalidateAll()
         cache.getIfPresent("wibble") shouldBe null
         cache.getIfPresent("wobble") shouldBe null
      }

      test("Cache should support contains") {
         val cache = Caffeine.newBuilder().asLoadingCache<String, String> {
            delay(1)
            "bar"
         }
         cache.put("wibble", "wobble")
         cache.contains("wibble") shouldBe true
         cache.contains("bubble") shouldBe false
      }

      test("support refresh") {
         var counter = 0
         val cache = Caffeine.newBuilder().asLoadingCache<String, Int> {
            counter++
            counter
         }
         cache.get("foo") shouldBe 1
         cache.refresh("foo") shouldBe 2
         eventually(5.seconds) {
            cache.get("foo") shouldBe 2
         }
      }

      test("support refresh all") {
         var counter = 0
         val cache = Caffeine.newBuilder().asLoadingCache<String, Int> {
            counter++
            counter
         }
         cache.get("foo") shouldBe 1
         cache.get("bar") shouldBe 2
         cache.refreshAll(setOf("foo", "bar")) shouldBe mapOf("foo" to 3, "bar" to 4)
         eventually(5.seconds) {
            cache.get("foo") shouldBe 3
            cache.get("bar") shouldBe 4
         }
      }

      test("check invariants on expire after") {
         val loggerExpiry = object : Expiry<Int, String> {
            override fun expireAfterRead(
               key: Int,
               value: String,
               currentTime: Long,
               currentDuration: Long
            ): Long {
               return 0
            }

            override fun expireAfterCreate(key: Int, value: String, currentTime: Long): Long {
               return 0
            }

            override fun expireAfterUpdate(
               key: Int,
               value: String,
               currentTime: Long,
               currentDuration: Long
            ): Long {
               return 0
            }
         }

         Caffeine.newBuilder()
            .maximumSize(2000)
            .initialCapacity(500)
            .expireAfter(loggerExpiry)
            .asLoadingCache<Int, String> {
               delay(1)
               "bar"
            }
      }
   }
}

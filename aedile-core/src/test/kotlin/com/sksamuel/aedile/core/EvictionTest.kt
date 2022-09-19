package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.RemovalCause
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.seconds

class EvictionTest : FunSpec() {
   init {
      test("cache should support eviction functions") {
         var cause: RemovalCause? = null
         val cache = caffeineBuilder()
            .maximumSize(1)
            .evictionListener { _, _, removalCause ->
               cause = removalCause
            }.build<String, String>()
         repeat(2) { k ->
            cache.put("$k") { "bar" }
         }
         eventually(5.seconds) {
            cause shouldBe RemovalCause.SIZE
         }
      }
   }
}

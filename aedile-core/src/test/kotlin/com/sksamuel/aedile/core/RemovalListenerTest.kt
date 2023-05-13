package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.RemovalCause
import io.kotest.assertions.timing.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class RemovalListenerTest : FunSpec() {
   init {
      test("cache should support removal listeners") {
         var cause: RemovalCause? = null
         val cache = caffeineBuilder<String, String> {
            maximumSize = 1
            removalListener = { _, _, removalCause -> cause = removalCause }
         }.build()
         repeat(2) { k ->
            cache.put("$k") { "bar" }
         }
         eventually(5.seconds) {
            cause shouldBe RemovalCause.SIZE
         }
      }

      test("cache should support suspendable removal listeners") {
         var cause: RemovalCause? = null
         val cache = caffeineBuilder<String, String> {
            maximumSize = 1
            removalListener = { _, _, removalCause ->
               delay(1)
               cause = removalCause
            }
         }.build()
         repeat(2) { k ->
            cache.put("$k") { "bar" }
         }
         eventually(5.seconds) {
            cause shouldBe RemovalCause.SIZE
         }
      }
   }
}

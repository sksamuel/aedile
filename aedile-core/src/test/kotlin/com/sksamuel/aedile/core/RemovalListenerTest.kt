package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class RemovalListenerTest : FunSpec() {
   init {
      test("cache should support removal listeners") {
         var c: RemovalCause? = null
         val cache = Caffeine.newBuilder()
            .maximumSize(1)
            .withRemovalListener { _, _, cause -> c = cause }
            .asCache<String, String>()
         repeat(2) { k ->
            cache.put("$k") { "bar" }
         }
         eventually(5.seconds) {
            c shouldBe RemovalCause.SIZE
         }
      }

      test("cache should support suspendable removal listeners") {
         var c: RemovalCause? = null
         val cache = Caffeine.newBuilder()
            .maximumSize(1)
            .withRemovalListener { _, _, cause ->
               delay(1)
               c = cause
            }
            .asCache<String, String>()

         repeat(2) { k ->
            cache.put("$k") { "bar" }
         }

         eventually(5.seconds) {
            c shouldBe RemovalCause.SIZE
         }
      }
   }
}

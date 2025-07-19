package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class SchedulerTest : FunSpec() {
   init {
      test("use custom scheduler") {
         var notified = false
         val cache = Caffeine.newBuilder()
            .expireAfterAccess(1.hours)
            .scheduler { command, duration ->
               notified = true
               CompletableDeferred()
            }.asCache<String, String>()
         notified shouldBe false
         cache.get("foo") { "bar" } shouldBe "bar"
         eventually(5.seconds) {
            notified shouldBe true
         }
      }
   }
}

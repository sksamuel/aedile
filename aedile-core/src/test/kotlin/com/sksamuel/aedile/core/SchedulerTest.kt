package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SchedulerTest : FunSpec() {
   init {
      test("use custom scheduler") {
         var notified = false
         val cache = Caffeine.newBuilder()
            .expireAfterAccess(1.hours)
            .scheduler(Scheduler { _, _ ->
               notified = true
               CompletableDeferred<Unit>()
            }).asCache<String, String>()
         notified shouldBe false
         cache.get("foo") { "bar" } shouldBe "bar"
         eventually(5.seconds) {
            notified shouldBe true
         }
      }

      test("scheduler maxDelay caps the scheduled duration") {
         var scheduledDuration: Duration? = null
         val cache = Caffeine.newBuilder()
            .expireAfterAccess(1.hours)
            .scheduler(
               scheduler = Scheduler { _, duration ->
                  scheduledDuration = duration
                  CompletableDeferred<Unit>()
               },
               maxDelay = 10.minutes,
            ).asCache<String, String>()
         cache.get("foo") { "bar" } shouldBe "bar"
         eventually(5.seconds) {
            scheduledDuration!! shouldBeLessThanOrEqualTo 10.minutes
         }
      }
   }
}

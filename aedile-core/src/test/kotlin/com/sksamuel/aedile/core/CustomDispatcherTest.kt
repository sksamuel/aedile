package com.sksamuel.aedile.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class CustomDispatcherTest : FunSpec() {
   init {
      test("cache should use custom dispatcher") {

         val dispatcher = Executors.newCachedThreadPool { r ->
            val t = Thread(r)
            t.name = "mcthreadface"
            t
         }.asCoroutineDispatcher()

         val cache = caffeineBuilder<String, String> {
            this.dispatcher = dispatcher
         }.build()
         cache.getOrPut("foo") {
            Thread.currentThread().name shouldContain "mcthreadface"
            "bar"
         } shouldBe "bar"
      }
   }
}

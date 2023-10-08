package com.sksamuel.aedile.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class CoroutineContextTest : FunSpec() {
   init {

      test("the calling context should be used by default for caches") {
         val cache = cacheBuilder<String, String?>().build()
         withContext(Hello()) {
            cache.get("foo") {
               "threadlocal=" + helloThreadLocal.get()
            } shouldBe "threadlocal=hello"

            cache.getAll(setOf("foo")) {
               it.associateWith { "threadlocal=" + helloThreadLocal.get() }
            } shouldBe mapOf("foo" to "threadlocal=hello")
         }
      }

      test("the calling context should be used by default for loading caches") {
         val cache = cacheBuilder<String, String?>().build { "yahoo" }
         withContext(Hello()) {
            cache.get("foo") {
               "$it=" + helloThreadLocal.get()
            } shouldBe "foo=hello"

            cache.getAll(setOf("foo")) {
               it.associateWith { "$it=" + helloThreadLocal.get() }
            } shouldBe mapOf("foo" to "foo=hello")
         }
      }

      test("the default scope should be used when delegating to the default builder") {
         val cache = cacheBuilder<String, String?>().build {
            "$it=" + helloThreadLocal.get()
         }
         withContext(Hello()) {
            cache.get("foo") {
               "$it=" + helloThreadLocal.get()
            } shouldBe "foo=hello"
            cache.get("bar") shouldBe "bar=null"
         }
      }

      test("the calling context should not be used when useCallingContext is false") {

         val cache = cacheBuilder<String, String?> {
            useCallingContext = false
         }.build()

         withContext(Hello()) {
            cache.get("foo") {
               "threadlocal=" + helloThreadLocal.get()
            } shouldBe "threadlocal=null"
         }
      }
   }
}

val helloThreadLocal: ThreadLocal<String?> = ThreadLocal.withInitial { null }

class Hello : ThreadContextElement<String?> {
   companion object HelloKey : CoroutineContext.Key<Hello>

   override val key: CoroutineContext.Key<Hello> = HelloKey

   override fun updateThreadContext(context: CoroutineContext): String? {
      return helloThreadLocal.get().also {
         helloThreadLocal.set("hello")
      }
   }

   override fun restoreThreadContext(context: CoroutineContext, oldState: String?) {
      helloThreadLocal.set(oldState)
   }
}

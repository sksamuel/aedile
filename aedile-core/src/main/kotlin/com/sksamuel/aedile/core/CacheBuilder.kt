package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.toJavaDuration

/**
 * Creates a [Builder] which by default uses the coroutine context from the caller for get/getall compute overrides.
 * To delegate to a global dispatcher, set the dispatcher in the configuration.
 */
fun <K, V> cacheBuilder(configure: Configuration<K, V>.() -> Unit = {}): Builder<K, V> {

   val c = Configuration<K, V>()
   c.configure()
   val caffeine = Caffeine.newBuilder()

   val defaultScope = c.scope ?: CoroutineScope(c.dispatcher + CoroutineName("Aedile-Caffeine-Scope") + SupervisorJob())

   c.evictionListener.let { listener ->
      caffeine.evictionListener<K, V> { key, value, cause ->
         defaultScope.launch {
            listener.invoke(key, value, cause)
         }
      }
   }

   c.removalListener.let { listener ->
      caffeine.removalListener<K, V> { key, value, cause ->
         defaultScope.launch {
            listener.invoke(key, value, cause)
         }
      }
   }

   c.initialCapacity?.let { caffeine.initialCapacity(it) }
   c.ticker?.let { caffeine.ticker(it) }

   c.maximumSize?.let { caffeine.maximumSize(it) }
   c.maximumWeight?.let { caffeine.maximumWeight(it) }
   c.weigher?.let { caffeine.weigher(it) }

   c.expireAfterWrite?.let { caffeine.expireAfterWrite(it.toJavaDuration()) }
   c.expireAfterAccess?.let { caffeine.expireAfterAccess(it.toJavaDuration()) }
   c.expireAfter?.let { caffeine.expireAfter(it) }

   c.refreshAfterWrite?.let { caffeine.refreshAfterWrite(it.toJavaDuration()) }
   c.statsCounter?.let { counter -> caffeine.recordStats { counter } }

   if (c.weakKeys == true) caffeine.weakKeys()
   if (c.softValues == true) caffeine.softValues()

   c.scheduler?.let { scheduler ->
      caffeine.scheduler { _, command, delay, unit ->
         scheduler.schedule(
            { command.run() },
            unit.toNanos(delay).nanoseconds,
         ).asCompletableFuture()
      }
   }

   return Builder(defaultScope, c.useCallingContext, caffeine)
}

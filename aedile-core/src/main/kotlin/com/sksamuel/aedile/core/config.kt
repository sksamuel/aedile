package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.toJavaDuration

/**
 * Specifies the scheduler to use when scheduling routine maintenance based on an expiration event.
 *
 * See full docs at [Caffeine.scheduler].
 */
fun <K, V> Caffeine<K, V>.scheduler(scheduler: Scheduler): Caffeine<K, V> {
   return scheduler { _, command, delay, unit ->
      scheduler.schedule(
         { command.run() },
         unit.toNanos(delay).nanoseconds,
      ).asCompletableFuture()
   }
}

/**
 * Specifies a listener that is notified each time an entry is removed.
 * See full docs at [Caffeine.removalListener].
 */
fun <K, V> Caffeine<K, V>.removalListener(
   scope: CoroutineScope,
   listener: suspend (K?, V?, RemovalCause) -> Unit,
): Caffeine<K, V> {
   return removalListener { key, value, cause ->
      scope.launch {
         listener.invoke(key, value, cause)
      }
   }
}

/**
 * Specifies a listener that is notified each time an entry is evicted.
 * See full docs at [Caffeine.evictionListener].
 */
fun <K, V> Caffeine<K, V>.evictionListener(
   scope: CoroutineScope,
   listener: suspend (K?, V?, RemovalCause) -> Unit,
): Caffeine<K, V> {
   return evictionListener { key, value, cause ->
      scope.launch {
         listener.invoke(key, value, cause)
      }
   }
}

/**
 * See full docs at [Caffeine.refreshAfterWrite].
 */
fun <K, V> Caffeine<K, V>.refreshAfterWrite(duration: Duration): Caffeine<K, V> {
   return this.refreshAfterWrite(duration.toJavaDuration())
}

/**
 * See full docs at [Caffeine.expireAfterAccess].
 */
fun <K, V> Caffeine<K, V>.expireAfterAccess(duration: Duration): Caffeine<K, V> {
   return this.expireAfterAccess(duration.toJavaDuration())
}

/**
 * See full docs at [Caffeine.expireAfterWrite].
 */
fun <K, V> Caffeine<K, V>.expireAfterWrite(duration: Duration): Caffeine<K, V> {
   return this.expireAfterWrite(duration.toJavaDuration())
}

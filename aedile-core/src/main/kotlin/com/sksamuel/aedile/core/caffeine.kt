package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.stats.StatsCounter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.toJavaDuration

data class Configuration<K, V>(

   /**
    * Sets the [CoroutineDispatcher] that is used when executing loading functions.
    */
   var dispatcher: CoroutineDispatcher = Dispatchers.IO,

   /**
    * The [CoroutineScope] that is used to create coroutines for loading functions and listeners.
    * If null, one will be created using the specified [dispatcher].
    */
   var scope: CoroutineScope? = null,

   var refreshAfterWrite: Duration? = null,
   var expireAfterAccess: Duration? = null,
   var expireAfterWrite: Duration? = null,

   var weakKeys: Boolean? = null,

   var maximumWeight: Long? = null,
   var maximumSize: Long? = null,

   var statsCounter: StatsCounter? = null,

   var expireAfter: Expiry<K, V>? = null,

   /**
    * Specifies a nanosecond-precision time source for use in determining when entries
    * should be expired or refreshed. By default, System.nanoTime is used.
    */
   var ticker: (() -> Long)? = null,

   /**
    * Specifies a listener that is notified each time an entry is evicted.
    *
    * The cache will invoke this listener during the atomic operation to remove the entry.
    * In the case of expiration or reference collection, the entry may be pending removal
    * and will be discarded as part of the routine maintenance.
    */
   var evictionListener: suspend (K?, V?, RemovalCause) -> Unit = { _, _, _ -> },

   /**
    * Sets the minimum total size for the internal data structures.
    *
    * Providing a large enough estimate at construction time avoids the
    * need for expensive resizing operations later,
    * but setting this value unnecessarily high wastes memory.
    */
   var initialCapacity: Int? = null,

   var weigher: ((K, V) -> Int)? = null,
)

/**
 * Creates a [Builder] which by default uses [Dispatchers.IO] to execute computation functions.
 */
fun <K, V> caffeineBuilder(configure: Configuration<K, V>.() -> Unit = {}): Builder<K, V> {

   val c = Configuration<K, V>()
   c.configure()
   val caffeine = Caffeine.newBuilder()

   val scope = c.scope ?: CoroutineScope(c.dispatcher + CoroutineName("Aedile-Caffeine-Scope") + SupervisorJob())

   c.evictionListener.let { listener ->
      caffeine.evictionListener<K, V> { key, value, cause ->
         scope.launch {
            listener.invoke(key, value, cause)
         }
      }
   }

   c.maximumSize?.let { caffeine.maximumSize(it) }
   c.maximumWeight?.let { caffeine.maximumWeight(it) }
   c.initialCapacity?.let { caffeine.initialCapacity(it) }
   c.weigher?.let { caffeine.weigher(it) }
   c.ticker?.let { caffeine.ticker(it) }

   c.expireAfterWrite?.let { caffeine.expireAfterWrite(it.toJavaDuration()) }
   c.expireAfterAccess?.let { caffeine.expireAfterAccess(it.toJavaDuration()) }
   c.expireAfter?.let { caffeine.expireAfter(it) }

   c.refreshAfterWrite?.let { caffeine.refreshAfterWrite(it.toJavaDuration()) }
   c.statsCounter?.let { counter -> caffeine.recordStats { counter } }

   if (c.weakKeys == true) caffeine.weakKeys()

   return Builder(scope, caffeine)
}

class Builder<K, V>(
   private val scope: CoroutineScope,
   private val caffeine: Caffeine<Any, Any>,
) {

   /**
    * Returns a [Cache] which suspends when requesting values.
    *
    * If the key is not present in the cache, returns null, unless a compute function
    * is provided with the key.
    *
    * If the suspendable computation throws or computes a null value then the
    * entry will be automatically removed.
    */
   fun build(): Cache<K, V> {
      return Cache(scope, caffeine.buildAsync())
   }

   /**
    * Returns a [Cache] which suspends when requesting values.
    *
    * If the key does not exist, then the suspendable [compute] function is invoked
    * to compute a value, unless a specific compute has been provided with the key.
    *
    * If the suspendable computation throws or computes a null value then the
    * entry will be automatically removed.
    *
    */
   fun build(compute: suspend (K) -> V): LoadingCache<K, V> {
      return LoadingCache(scope, caffeine.buildAsync { key, _ -> scope.async { compute(key) }.asCompletableFuture() })
   }
}


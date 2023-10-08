package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Expiry
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.stats.StatsCounter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration

data class Configuration<K, V>(

   /**
    * Sets the [CoroutineDispatcher] that is used when executing default build functions.
    */
   var dispatcher: CoroutineDispatcher = Dispatchers.IO,

   /**
    * When set to true, the default, compute function overrides (those provided when invoking get/getAll)
    * will use the callers coroutine context. When set to false, will use the [dispatcher] value.
    */
   var useCallingContext: Boolean = true,

   /**
    * The [CoroutineScope] that is used to create coroutines for loading functions and listeners.
    * If null, one will be created using the specified [dispatcher].
    */
   var scope: CoroutineScope? = null,

   /**
    * See full docs at [Caffeine.refreshAfterWrite].
    */
   var refreshAfterWrite: Duration? = null,

   /**
    * See full docs at [Caffeine.expireAfterAccess].
    */
   var expireAfterAccess: Duration? = null,

   /**
    * See full docs at [Caffeine.expireAfterWrite].
    */
   var expireAfterWrite: Duration? = null,

   /**
    * Specifies that each key (not value) stored in the cache should be wrapped in a WeakReference.
    * See full docs at [Caffeine.weakKeys].
    */
   var weakKeys: Boolean? = null,

   /**
    * Specifies that each value (not key) stored in the cache should be wrapped in a SoftReference.
    * See full docs at [Caffeine.softValues].
    */
   var softValues: Boolean? = null,

   /**
    * See full docs at [Caffeine.maximumWeight].
    */
   var maximumWeight: Long? = null,

   /**
    * See full docs at [Caffeine.maximumSize].
    */
   var maximumSize: Long? = null,

   var statsCounter: StatsCounter? = null,

   /**
    * See full docs at [Caffeine.expireAfter].
    */
   var expireAfter: Expiry<K, V>? = null,

   /**
    * Specifies a nanosecond-precision time source for use in determining when entries
    * should be expired or refreshed. By default, System.nanoTime is used.
    *
    * See full docs at [Caffeine.ticker].
    */
   var ticker: (() -> Long)? = null,

   /**
    * Specifies a listener that is notified each time an entry is evicted.
    * See full docs at [Caffeine.evictionListener].
    */
   var evictionListener: suspend (K?, V?, RemovalCause) -> Unit = { _, _, _ -> },

   /**
    * Specifies a listener that is notified each time an entry is removed.
    * See full docs at [Caffeine.removalListener].
    */
   var removalListener: suspend (K?, V?, RemovalCause) -> Unit = { _, _, _ -> },

   /**
    * Sets the minimum total size for the internal data structures.
    *
    * Providing a large enough estimate at construction time avoids the
    * need for expensive resizing operations later,
    * but setting this value unnecessarily high wastes memory.
    *
    * See full docs at [Caffeine.initialCapacity].
    */
   var initialCapacity: Int? = null,

   /**
    * Specifies the weigher to use in determining the weight of entries.
    * Entry weight is taken into consideration by maximumWeight(long) when determining which entries to evict.
    *
    * See full docs at [Caffeine.weigher].
    */
   var weigher: ((K, V) -> Int)? = null,

   var scheduler: Scheduler? = null,
)

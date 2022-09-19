package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.util.concurrent.Executor
import kotlin.time.Duration
import kotlin.time.toJavaDuration

data class Configuration<K, V>(

   /**
    * Sets the [CoroutineDispatcher] that is used when executing loading functions.
    */
   var dispatcher: CoroutineDispatcher = Dispatchers.IO,

   var refreshAfterWrite: Duration? = null,
   var expireAfterAccess: Duration? = null,
   var expireAfterWrite: Duration? = null,

   var maximumWeight: Long? = null,
   var maximumSize: Long? = null,

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
   var evictionListener: (K?, V?, RemovalCause) -> Unit = { _, _, _ -> },

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

   c.evictionListener.let { caffeine.evictionListener(it) }
   c.maximumSize?.let { caffeine.maximumSize(it) }
   c.maximumWeight?.let { caffeine.maximumWeight(it) }
   c.initialCapacity?.let { caffeine.initialCapacity(it) }
   c.weigher?.let { caffeine.weigher(it) }
   c.ticker?.let { caffeine.ticker(it) }
   c.expireAfterWrite?.let { caffeine.expireAfterWrite(it.toJavaDuration()) }
   c.expireAfterAccess?.let { caffeine.expireAfterAccess(it.toJavaDuration()) }
   c.refreshAfterWrite?.let { caffeine.refreshAfterWrite(it.toJavaDuration()) }

   val scope = CoroutineScope(c.dispatcher + CoroutineName("Aedile-Caffeine-Scope"))
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

class Cache<K, V>(private val scope: CoroutineScope, private val cache: AsyncCache<K, V>) {

   fun underlying(): AsyncCache<K, V> = cache

   suspend fun getIfPresent(key: K): V? {
      return cache.getIfPresent(key)?.await()
   }

   /**
    * Returns the value associated with key in this cache, obtaining that value from the
    * [compute] function if necessary. This method provides a simple substitute for the conventional
    * "if cached, return; otherwise create, cache and return" pattern.
    *
    * The instance returned from the compute function will be stored directly into the cache.
    *
    * If the specified key is not already associated with a value, attempts to compute its value
    * and enters it into this cache unless null.
    *
    * If the suspendable computation throws, the exception will be propagated to the caller.
    *
    * @param key the key to lookup in the cache
    * @param compute the suspendable function to generate a value for the given key.
    * @return the present value, the computed value, or throws.
    *
    */
   suspend fun getOrPut(key: K, compute: suspend (K) -> V): V {
      return cache.get(key) { k, _ -> scope.async { compute(k) }.asCompletableFuture() }.await()
   }

   /**
    * Associates a computed value with the given [key] in this cache.
    *
    * If the cache previously contained a value associated with key,
    * the old value is replaced by the new value.
    *
    * If the suspendable computation throws, the entry will be automatically removed.
    *
    * @param key the key to associate the computed value with
    * @param compute the suspendable function that generate the value.
    */
   suspend fun put(key: K, compute: suspend () -> V) {
      cache.put(key, scope.async { compute() }.asCompletableFuture())
   }

   suspend fun asMap(): Map<K, V> {
      return cache.asMap().mapValues { it.value.await() }
   }

   suspend fun getAll(keys: Collection<K>, compute: suspend (Collection<K>) -> Map<K, V>): Map<K, V> {
      return cache.getAll(
         keys
      ) { ks: Set<K>, _: Executor ->
         scope.async { compute(ks) }.asCompletableFuture()
      }.await()
   }
}

class LoadingCache<K, V>(private val scope: CoroutineScope, private val cache: AsyncLoadingCache<K, V>) {

   fun underlying(): AsyncLoadingCache<K, V> = cache

   suspend fun getIfPresent(key: K): V? {
      return cache.getIfPresent(key)?.await()
   }

   /**
    * Returns the value associated with key in this cache, suspending while that value is computed
    * if necessary. If the computation throws, the entry will be automatically removed from this cache.
    *
    * @param key the key to lookup in the cache
    * @return the value in the cache or computed from the global loading function.
    */
   suspend fun get(key: K): V {
      return cache.get(key).await()
   }

   suspend fun getAll(keys: Collection<K>): Map<K, V> {
      return cache.getAll(keys).await()
   }

   /**
    * Returns the value associated with key in this cache, obtaining that value from the
    * [compute] function if necessary. This method provides a simple substitute for the conventional
    * "if cached, return; otherwise create, cache and return" pattern.
    *
    * The instance returned from the compute function will be stored directly into the cache.
    *
    * If the specified key is not already associated with a value, attempts to compute its value
    * and enters it into this cache unless null.
    *
    * If the suspendable computation throws, the entry will be automatically removed from this cache.
    */
   suspend fun getOrPut(key: K, compute: suspend (K) -> V): V {
      return cache.get(key) { k, _ -> scope.async { compute(k) }.asCompletableFuture() }.await()
   }

   /**
    * Associates a computed value with the given [key] in this cache.
    *
    * If the cache previously contained a value associated with key,
    * the old value is replaced by the new value.
    *
    * If the suspendable computation throws, the entry will be automatically removed.
    *
    * @param key the key to associate the computed value with
    * @param compute the suspendable function that generate the value.
    */
   suspend fun put(key: K, compute: suspend () -> V) {
      cache.put(key, scope.async { compute() }.asCompletableFuture())
   }

   suspend fun asMap(): Map<K, V> {
      return cache.asMap().mapValues { it.value.await() }
   }
}


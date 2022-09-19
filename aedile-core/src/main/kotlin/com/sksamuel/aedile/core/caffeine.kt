package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import com.github.benmanes.caffeine.cache.Weigher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun caffeineBuilder(): Builder<Any, Any> {
   return Builder(Caffeine.newBuilder())
}

class Builder<K, V>(private val builder: Caffeine<K, V>) {

   private var scope = createScope(Dispatchers.IO)

   private fun createScope(dispatcher: CoroutineDispatcher): CoroutineScope {
      return CoroutineScope(dispatcher + CoroutineName("Aedile-Caffeine-Scope"))
   }

   /**
    * Sets the [CoroutineDispatcher] that is used when executing loading functions.
    */
   fun withDispatcher(dispatcher: CoroutineDispatcher): Builder<K, V> {
      scope = createScope(dispatcher)
      return this
   }

   fun refreshAfterWrite(duration: Duration): Builder<K, V> {
      builder.refreshAfterWrite(duration.toJavaDuration())
      return this
   }

   fun expireAfterAccess(duration: Duration): Builder<K, V> {
      builder.expireAfterAccess(duration.toJavaDuration())
      return this
   }

   fun expireAfterWrite(duration: Duration): Builder<K, V> {
      builder.expireAfterWrite(duration.toJavaDuration())
      return this
   }

   fun maximumWeight(maximumWeight: Long): Builder<K, V> {
      builder.maximumWeight(maximumWeight)
      return this
   }

   fun <K1 : K, V1 : V> maximumWeight(weigher: (K1, V1) -> Int): Builder<K, V> {
      builder.weigher(Weigher<K1, V1> { key, value -> weigher(key, value) })
      return this
   }

   fun maximumSize(maximumSize: Long): Builder<K, V> {
      builder.maximumSize(maximumSize)
      return this
   }

   /**
    * Specifies a nanosecond-precision time source for use in determining when entries
    * should be expired or refreshed. By default, System.nanoTime is used.
    *
    * @param nano returns the current nanosecond value to use.
    */
   fun ticker(nano: () -> Long): Builder<K, V> {
      builder.ticker { nano() }
      return this
   }

   /**
    * Sets the minimum total size for the internal data structures.
    *
    * Providing a large enough estimate at construction time avoids the
    * need for expensive resizing operations later,
    * but setting this value unnecessarily high wastes memory.
    *
    * @param initialCapacity the minimum and starting size of the cache.
    */
   fun initialCapacity(initialCapacity: Int): Builder<K, V> {
      builder.initialCapacity(initialCapacity)
      return this
   }

   /**
    * Specifies a listener that is notified each time an entry is evicted.
    *
    * The cache will invoke this listener during the atomic operation to remove the entry.
    * In the case of expiration or reference collection, the entry may be pending removal
    * and will be discarded as part of the routine maintenance.
    *
    * @param listener the listener to invoke with the key, value and cause.
    */
   fun evictionListener(listener: (K?, V?, RemovalCause) -> Unit): Builder<K, V> {
      builder.evictionListener(RemovalListener<K, V> { key, value, cause -> listener(key, value, cause) })
      return this
   }

   /**
    * Returns a [Cache] which suspends when requesting values.
    *
    * If the key is not present in the cache, returns null, unless a compute function
    * is provided with the key.
    *
    * If the suspendable computation throws or computes a null value then the
    * entry will be automatically removed.
    */
   fun <K1 : K, V1 : V> build(): Cache<K1, V1> {
      return Cache(scope, builder.buildAsync())
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
   fun <K1 : K, V1 : V> build(compute: suspend (K1) -> V1): LoadingCache<K1, V1> {
      return LoadingCache(scope, builder.buildAsync { key, _ -> scope.async { compute(key) }.asCompletableFuture() })
   }
}

class Cache<K, V>(private val scope: CoroutineScope, private val cache: AsyncCache<K, V>) {

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

class LoadingCache<K, V>(private val scope: CoroutineScope, private val cache: AsyncLoadingCache<K, V>) {

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


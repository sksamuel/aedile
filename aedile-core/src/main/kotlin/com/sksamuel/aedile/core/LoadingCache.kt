package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.coroutineContext

class LoadingCache<K : Any, V>(
   private val cache: AsyncLoadingCache<K, V>
) {

   fun underlying(): AsyncLoadingCache<K, V> = cache

   suspend fun contains(key: K): Boolean {
      return cache.getIfPresent(key)?.await() != null
   }

   /**
    * Returns the value associated with key in this cache, or null if there is no cached future for key.
    * This method will suspend while the value is fetched.
    */
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

   /**
    * Returns the values associated with the given [keys] from this cache, suspending while each value
    * is computed if necessary. If the computation throws, the entry will be automatically removed from this cache.
    *
    * @param keys the keys to lookup in the cache
    * @return the values in the cache or computed from the global loading function.
    *
    * See full docs at [AsyncLoadingCache.getAll].
    */
   suspend fun getAll(keys: Collection<K>): Map<K, V> {
      return cache.getAll(keys).await()
   }

   /**
    * Returns the values associated with the given [keys] from this cache, obtaining those values
    * from the given [compute] function where required.
    *
    * If the computation throws, these entries will be removed from the cache, and the exception propagated.
    *
    * @param keys the keys to lookup in the cache
    * @param compute the function to calculate missing keys
    * @return the values in the cache or computed from the global loading function.
    *
    * See full docs at [AsyncCache.getAll].
    */
   suspend fun getAll(keys: Collection<K>, compute: suspend (Set<K>) -> Map<K, V>): Map<K, V> {
      val scope = CoroutineScope(coroutineContext)
      return cache.getAll(keys) { k, _ -> scope.async { compute(k.toSet()) }.asCompletableFuture() }.await()
   }

   /**
    * Returns the value associated with a key in this cache, getting that value from the
    * [compute] function if necessary. This method provides a simple substitute for the conventional
    * "if cached, return; otherwise create, cache, and return" pattern.
    *
    * The instance returned from the compute function will be stored directly into the cache.
    *
    * If the specified key is not already associated with a value, attempts to compute its value
    * and enters it into this cache unless null.
    *
    * If the suspendable computation throws, the entry will be automatically removed from this cache.
    *
    * See full docs at [AsyncLoadingCache.get].
    */
   suspend fun get(key: K, compute: suspend (K) -> V): V {

      val scope = CoroutineScope(coroutineContext)
      var error: Throwable? = null
      val value = cache.get(key) { k, _ ->
         val asCompletableFuture = scope.async {
            // if compute throws, then it will cause the parent coroutine to be cancelled as well
            // we don't want that, as want to throw the exception back to the caller.
            // so we must capture it and throw it manually
            try {
               compute(k)
            } catch (e: Throwable) {
               error = e
               null
            }
         }.asCompletableFuture()
         asCompletableFuture.thenApply { it ?: throw error ?: NullPointerException() }
      }.await()
      error?.let { throw it }
      return value
   }

   /**
    * Returns the value associated with a key in this cache, getting that value from the
    * [compute] function if necessary. This function will suspend while the compute method
    * is executed. If the suspendable computation throws, the exception will be propagated to the caller.
    *
    * If the specified key is not already associated with a value, attempts to compute its value asynchronously
    * and enters it into this cache unless null.
    *
    * @param key the key to lookup in the cache
    * @param compute the suspendable function to generate a value for the given key.
    * @return the present value, the computed value, or throws.
    */
   suspend fun getOrNull(key: K, compute: suspend (K) -> V?): V? {
      val scope = CoroutineScope(coroutineContext)
      return cache.get(key) { k, _ -> scope.async { compute(k) }.asCompletableFuture() }.await()
   }

   @Deprecated("Use get", ReplaceWith("get(key, compute)"))
   suspend fun getOrPut(key: K, compute: suspend (K) -> V): V = get(key, compute)

   /**
    * Associates a computed value with the given [key] in this cache.
    *
    * If the cache previously contained a value associated with key,
    * the old value is replaced by the new value.
    *
    * If the suspendable computation throws, the entry will be automatically removed.
    *
    * @param key the key to associate the computed value with
    * @param compute the suspendable function that generates the value.
    */
   suspend fun put(key: K, compute: suspend () -> V) {
      val scope = CoroutineScope(coroutineContext)
      cache.put(key, scope.async { compute() }.asCompletableFuture())
   }

   /**
    * Associates a computed value with the given [key] in this cache.
    *
    * If the cache previously contained a value associated with key,
    * the old value is replaced by the new value.
    */
   fun put(key: K, value: V) {
      cache.put(key, CompletableFuture.completedFuture(value))
   }

   /**
    * Equivalent to [put], but exists so that we can override the operator.
    */
   operator fun set(key: K, value: V) = put(key, value)

   /**
    * Returns a view of the entries in this map, requesting each value before returning the map.
    * Note: This requires a fetch on all keys before the map is returned. For a lazily built
    * map where each key is fetched as a suspendable call on demand, see [asDeferredMap].
    */
   suspend fun asMap(): Map<K, V> {
      return cache.asMap().mapValues { it.value.await() }
   }

   /**
    * Returns a view of the entries stored in this cache as an immutable map.
    * Each value is fetched from the cache only when requested from the map.
    * */
   fun asDeferredMap(): Map<K, Deferred<V>> {
      return cache.asMap().mapValues { it.value.asDeferred() }
   }

   /**
    * Discards the given key in the cache.
    * Will block until completed.
    * The behavior of the entry if currently being loaded is undefined.
    */
   fun invalidate(key: K) {
      cache.synchronous().invalidate(key)
   }

   /**
    * Discards all entries in the cache.
    * Will block until completed.
    * The behavior of entries currently being loaded is undefined.
    */
   fun invalidateAll() {
      cache.synchronous().invalidateAll()
   }

   /**
    * Loads a new value for the key, asynchronously. While the new value is loading, the
    * previous value (if any) will continue to be returned by get(key) unless it is evicted.
    *
    * See full docs at [com.github.benmanes.caffeine.cache.LoadingCache.refresh].
    */
   suspend fun refresh(key: K): V {
      return cache.synchronous().refresh(key).await()
   }

   /**
    * Loads a new value for each key, asynchronously. While the new value is loading, the
    * previous value (if any) will continue to be returned by get(key) unless it is evicted.
    *
    * See full docs at [com.github.benmanes.caffeine.cache.LoadingCache.refreshAll].
    */
   suspend fun refreshAll(keys: Collection<K>): Map<K, V> {
      return cache.synchronous().refreshAll(keys).await()
   }
}

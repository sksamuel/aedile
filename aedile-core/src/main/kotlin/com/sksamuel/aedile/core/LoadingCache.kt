package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture

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

   /**
    * Returns the values associated with the given [keys] from this cache, suspending while each value
    * is computed if necessary. If the computation throws, the entry will be automatically removed from this cache.
    *
    * @param keys the keys to lookup in the cache
    * @return the values in the cache or computed from the global loading function.
    */
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
   suspend fun get(key: K, compute: suspend (K) -> V): V {
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
    * @param compute the suspendable function that generate the value.
    */
   suspend fun put(key: K, compute: suspend () -> V) {
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
}

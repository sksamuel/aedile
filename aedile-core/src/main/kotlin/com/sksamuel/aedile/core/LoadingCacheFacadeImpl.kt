package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await

public class LoadingCacheFacadeImpl<K, V>(
   private val scope: CoroutineScope,
   private val cache: AsyncLoadingCache<K, V>,
) : LoadingCacheFacade<K, V> {

   override fun underlying(): AsyncLoadingCache<K, V> = cache

   override suspend fun contains(key: K): Boolean {
      return cache.getIfPresent(key)?.await() != null
   }

   /**
    * Returns the future associated with [key] in this cache, or `null` if there is no
    * cached future for [key].
    */
   override suspend fun getIfPresent(key: K): V? {
      return cache.getIfPresent(key)?.await()
   }

   /**
    * Returns the value associated with key in this cache, suspending while that value is computed
    * if necessary. If the computation throws, the entry will be automatically removed from this cache.
    *
    * @param key the key to lookup in the cache
    * @return the value in the cache or computed from the global loading function.
    */
   override suspend fun get(key: K): V {
      return cache.get(key).await()
   }

   /**
    * Returns the future of a map of the values associated with [keys], creating or retrieving
    * those values if necessary. The returned map contains entries that were already cached, combined
    * with newly loaded entries; it will never contain null keys or values. If the any of the
    * asynchronous computations fail, those entries will be automatically removed from this cache.
    *
    * Note that duplicate elements in [keys], as determined by [Any.equals], will be ignored.
    */
   override suspend fun getAll(keys: Collection<K>, compute: suspend (Collection<K>) -> Map<K, V>): Map<K, V> {
      return cache.getAll(
         keys
      ) { ks: Set<K>, _: Executor ->
         scope.async { compute(ks) }.asCompletableFuture()
      }.await()
   }

   /**
    * Returns the values associated with the given [keys] from this cache, suspending while each value
    * is computed if necessary. If the computation throws, the entry will be automatically removed from this cache.
    *
    * @param keys the keys to lookup in the cache
    * @return the values in the cache or computed from the global loading function.
    */
   override suspend fun getAll(keys: Collection<K>): Map<K, V> {
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
   override suspend fun get(key: K, compute: suspend (K) -> V): V {
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
   override suspend fun put(key: K, compute: suspend () -> V) {
      cache.put(key, scope.async { compute() }.asCompletableFuture())
   }

   /**
    * Associates a computed value with the given [key] in this cache.
    *
    * If the cache previously contained a value associated with key,
    * the old value is replaced by the new value.
    */
   override fun put(key: K, value: V) {
      cache.put(key, CompletableFuture.completedFuture(value))
   }

   /**
    * Returns a view of the entries in this map, requesting each value before returning the map.
    * Note: This requires a fetch on all keys before the map is returned. For a lazily built
    * map where each key is fetched as a suspendable call on demand, see [asDeferredMap].
    */
   override suspend fun asMap(): Map<K, V> {
      return cache.asMap().mapValues { it.value.await() }
   }

   /**
    * Returns a view of the entries stored in this cache as an immutable map.
    * Each value is fetched from the cache only when requested from the map.
    */
   override fun asDeferredMap(): Map<K, Deferred<V>> {
      return cache.asMap().mapValues { it.value.asDeferred() }
   }

   /**
    * Discards the given key in the cache.
    * Will block until completed.
    * Behavior of the entry if currently being loaded is undefined.
    */
   override fun invalidate(key: K) {
      cache.synchronous().invalidate(key)
   }

   /**
    * Discards all entries in the cache.
    * Will block until completed.
    * Behavior of entries currently being loaded is undefined.
    */
   override fun invalidateAll() {
      cache.synchronous().invalidateAll()
   }
}

package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await

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

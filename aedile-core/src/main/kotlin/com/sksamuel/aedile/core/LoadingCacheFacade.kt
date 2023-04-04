package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncLoadingCache

@Deprecated(
   message = "Divided to api and implementation",
   replaceWith = ReplaceWith(
      "LoadingCacheWrapper<K, V>",
      "com.sksamuel.aedile.core.LoadingCacheWrapper"
   ),
   level = DeprecationLevel.ERROR,
)
typealias LoadingCache<K, V> = LoadingCacheFacade<K, V>

interface LoadingCacheFacade<K, V> : CacheFacade<K, V> {

   override fun underlying(): AsyncLoadingCache<K, V>

   /**
    * Returns the value associated with key in this cache, suspending while that value is computed
    * if necessary. If the computation throws, the entry will be automatically removed from this cache.
    *
    * @param key the key to lookup in the cache
    * @return the value in the cache or computed from the global loading function.
    */
   suspend fun get(key: K): V

   /**
    * Returns the values associated with the given [keys] from this cache, suspending while each value
    * is computed if necessary. If the computation throws, the entry will be automatically removed from this cache.
    *
    * @param keys the keys to lookup in the cache
    * @return the values in the cache or computed from the global loading function.
    */
   suspend fun getAll(keys: Collection<K>): Map<K, V>
}

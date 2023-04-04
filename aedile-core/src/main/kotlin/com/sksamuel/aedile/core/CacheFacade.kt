package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncCache
import kotlinx.coroutines.Deferred

@Deprecated(
   message = "Divided to api and implementation",
   replaceWith = ReplaceWith(
      "CacheWrapper<K, V>",
      "com.sksamuel.aedile.core.CacheWrapper"
   ),
   level = DeprecationLevel.ERROR,
)
typealias Cache<K, V> = CacheFacade<K, V>

interface CacheFacade<K, V> {

   fun underlying(): AsyncCache<K, V>

   suspend fun contains(key: K): Boolean

   suspend fun getIfPresent(key: K): V?

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
   suspend fun get(key: K, compute: suspend (K) -> V): V

   @Deprecated("use get", ReplaceWith("get(key, compute)"))
   suspend fun getOrPut(key: K, compute: suspend (K) -> V): V = get(key, compute)

   /**
    * Associates a computed value with the given [key] in this cache.
    *
    * If the cache previously contained a value associated with key,
    * the old value is replaced by the new value.
    */
   fun put(key: K, value: V)

   /**
    * Equivalent to [put], but exists so that we can override the operator.
    */
   operator fun set(key: K, value: V) = put(key, value)

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
   suspend fun put(key: K, compute: suspend () -> V)

   /**
    * Returns a view of the entries in this map, requesting each value before returning the map.
    * Note: This requires a fetch on all keys before the map is returned. For a lazily built
    * map where each key is fetched as a suspendable call on demand, see [asDeferredMap].
    */
   suspend fun asMap(): Map<K, V>

   /**
    * Returns a view of the entries stored in this cache as an immutable map.
    * Each value is fetched from the cache only when requested from the map.
    */
   fun asDeferredMap(): Map<K, Deferred<V>>

   /**
    * Returns the future of a map of the values associated with [keys], creating or retrieving
    * those values if necessary. The returned map contains entries that were already cached, combined
    * with newly loaded entries; it will never contain null keys or values. If the any of the
    * asynchronous computations fail, those entries will be automatically removed from this cache.
    *
    * Note that duplicate elements in [keys], as determined by [Any.equals], will be ignored.
    */
   suspend fun getAll(keys: Collection<K>, compute: suspend (Collection<K>) -> Map<K, V>): Map<K, V>

   /**
    * Discards the given key in the cache.
    * Will block until completed.
    * Behavior of the entry if currently being loaded is undefined.
    */
   fun invalidate(key: K)

   /**
    * Discards all entries in the cache.
    * Will block until completed.
    * Behavior of entries currently being loaded is undefined.
    */
   fun invalidateAll()
}

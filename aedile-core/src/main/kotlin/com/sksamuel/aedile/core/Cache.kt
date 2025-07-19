package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext

class Cache<K : Any, V : Any>(
   private val cache: AsyncCache<K, V>
) {

   fun underlying(): AsyncCache<K, V> = cache

   suspend fun contains(key: K): Boolean {
      return cache.getIfPresent(key)?.await() != null
   }

   /**
    * Returns the value associated with a key in this cache, or null if there is no cached future for the key.
    * This method will suspend while the value is fetched.
    * For a non-suspending alternative, see [getOrNull].
    */
   suspend fun getIfPresent(key: K): V? {
      return cache.getIfPresent(key)?.await()
   }

   /**
    * Returns the value associated with a key in this cache or null if this cache does not
    * contain an entry for the key. This is a non-suspendable alternative to getIfPresent(key).
    */
   fun getOrNull(key: K): V? {
      return cache.synchronous().getIfPresent(key)
   }

   /**
    * Returns the value associated with a key in this cache, getting that value from the
    * [compute] function if necessary. This function will suspend while the compute method
    * is executed.
    *
    * If the suspendable computation throws, the exception will be propagated to the caller.
    *
    * See full docs at [AsyncCache.get].
    *
    * @param key the key to lookup in the cache
    * @param compute the suspendable function to generate a value for the given key.
    * @return the present value, the computed value, or throws.
    *
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
    * and enters it into this cache unless null. The entire method invocation is performed atomically, so the
    * function is applied at most once per key. If the asynchronous computation fails, the entry will be
    * automatically removed from this cache.
    *
    * @param key the key to lookup in the cache
    * @param compute the suspendable function to generate a value for the given key.
    * @return the present value, the computed value, or throws.
    */
   suspend fun getOrNull(key: K, compute: suspend (K) -> V?): V? {
      val scope = CoroutineScope(coroutineContext)
      return cache.get(key) { k, _ -> scope.async { compute(k) }.asCompletableFuture() }.await()
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

   suspend fun getAll(keys: Collection<K>, compute: suspend (Collection<K>) -> Map<K, V>): Map<K, V> {
      val scope = CoroutineScope(coroutineContext)
      var error: Throwable? = null
      val value = cache.getAll(keys) { ks: Set<K>, _: Executor ->
         scope.async {
            // if compute throws, then it will cause the parent coroutine to be cancelled as well
            // we don't want that, as want to throw the exception back to the caller.
            // so we must capture it and throw it manually
            try {
               compute(ks)
            } catch (e: Throwable) {
               error = e
               emptyMap()
            }
         }.asCompletableFuture()
      }.await()
      error?.let { throw it }
      return value
   }

   /**
    * Discards the given key in the cache.
    * Will block until completed.
    * Behavior of the entry if currently being loaded is undefined.
    */
   fun invalidate(key: K) {
      cache.synchronous().invalidate(key)
   }

   /**
    * Discards all entries in the cache.
    * Will block until completed.
    * Behavior of entries currently being loaded is undefined.
    */
   fun invalidateAll() {
      cache.synchronous().invalidateAll()
   }
}

package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncCacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Returns a [Cache] which suspends when requesting values.
 *
 * If the key is not present in the cache, returns null, unless a compute function
 * is provided with the key.
 *
 * If the suspendable computation throws or computes a null value then the
 * entry will be automatically removed.
 */
fun <K : Any?, V : Any?> Caffeine<in K, in V>.asCache(): Cache<K, V> {
   val scope = CoroutineScope(Dispatchers.IO + CoroutineName("Aedile-AsyncLoadingCache-Scope") + SupervisorJob())
   return asCache(scope)
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
fun <K : Any?, V : Any?> Caffeine<in K, in V>.asCache(scope: CoroutineScope): Cache<K, V> {
   return Cache(scope, true, buildAsync())
}

/**
 * Returns a [LoadingCache] which uses the provided [compute] function
 * to compute a value, unless a specific compute has been provided with the key.
 *
 * If the suspendable computation throws or computes a null value then the
 * entry will be automatically removed.
 */
fun <K : Any?, V : Any?> Caffeine<in K, in V>.asLoadingCache(compute: suspend (K) -> V): LoadingCache<K, V> {
   val scope = CoroutineScope(Dispatchers.IO + CoroutineName("Aedile-AsyncLoadingCache-Scope") + SupervisorJob())
   return asLoadingCache(scope, compute)
}

/**
 * Returns a [LoadingCache] which uses the provided [compute] function
 * to compute a value, unless a specific compute has been provided with the key.
 *
 * The compute function will execute on the given [scope].
 *
 * If the suspendable computation throws or computes a null value then the
 * entry will be automatically removed.
 */
fun <K : Any?, V : Any?> Caffeine<in K, in V>.asLoadingCache(
   scope: CoroutineScope,
   compute: suspend (K) -> V
): LoadingCache<K, V> {
   return LoadingCache(scope, true, buildAsync { key, _ -> scope.async { compute(key) }.asCompletableFuture() })
}

/**
 * Returns an [LoadingCache] which suspends when requesting values.
 *
 * If the key does not exist, then the suspendable [compute] function is invoked
 * to compute a value, unless a specific compute has been provided with the key.
 *
 * If the suspendable computation throws or computes a null value then the
 * entry will be automatically removed.
 *
 * The [reloadCompute] function is invoked to refresh an entry if refreshAfterWrite
 * is enabled or refresh is invoked. See full docs [AsyncCacheLoader.asyncReload].
 */
fun <K : Any?, V : Any?> Caffeine<in K, in V>.asLoadingCache(
   compute: suspend (K) -> V,
   reloadCompute: suspend (K, V) -> V,
): LoadingCache<K, V> {
   val scope = CoroutineScope(Dispatchers.IO + CoroutineName("Aedile-AsyncLoadingCache-Scope") + SupervisorJob())
   return asLoadingCache(scope, compute, reloadCompute)
}

/**
 * Returns an [LoadingCache] which suspends when requesting values.
 *
 * If the key does not exist, then the suspendable [compute] function is invoked
 * to compute a value, unless a specific compute has been provided with the key.
 *
 * If the suspendable computation throws or computes a null value then the
 * entry will be automatically removed.
 *
 * The [reloadCompute] function is invoked to refresh an entry if refreshAfterWrite
 * is enabled or refresh is invoked. See full docs [AsyncCacheLoader.asyncReload].
 *
 * The compute functions will execute on the given [scope].
 */
fun <K : Any?, V : Any?> Caffeine<in K, in V>.asLoadingCache(
   scope: CoroutineScope,
   compute: suspend (K) -> V,
   reloadCompute: suspend (K, V) -> V,
): LoadingCache<K, V> {
   return LoadingCache(scope, true, buildAsync(object : AsyncCacheLoader<K, V> {
      override fun asyncLoad(key: K, executor: Executor?): CompletableFuture<out V> {
         return scope.async { compute(key) }.asCompletableFuture()
      }

      override fun asyncReload(key: K, oldValue: V, executor: Executor?): CompletableFuture<out V> {
         return scope.async { reloadCompute(key, oldValue) }.asCompletableFuture()
      }
   }))
}

/**
 * Returns an [LoadingCache] which suspends when requesting values.
 *
 * If a requested key does not exist, then the suspendable [compute] function is invoked
 * to compute the required values.
 *
 * If the suspendable computation throws or computes a null value then the
 * entry will be automatically removed.
 */
fun <K : Any?, V : Any?> Caffeine<in K, in V>.asBulkLoadingCache(compute: suspend (Set<K>) -> Map<K, V>): LoadingCache<K, V> {
   val scope = CoroutineScope(Dispatchers.IO + CoroutineName("Aedile-AsyncLoadingCache-Scope") + SupervisorJob())
   return asBulkLoadingCache(scope, compute)
}

/**
 * Returns an [LoadingCache] which suspends when requesting values.
 *
 * If a requested key does not exist, then the suspendable [compute] function is invoked
 * to compute the required values.
 *
 * If the suspendable computation throws or computes a null value then the
 * entry will be automatically removed.
 *
 * The compute function will execute on the given [scope].
 */
fun <K : Any?, V : Any?> Caffeine<in K, in V>.asBulkLoadingCache(
   scope: CoroutineScope,
   compute: suspend (Set<K>) -> Map<K, V>
): LoadingCache<K, V> {
   return LoadingCache(scope, true, buildAsync(AsyncCacheLoader.bulk { keys, _ ->
      scope.async { compute(keys) }.asCompletableFuture()
   }))
}

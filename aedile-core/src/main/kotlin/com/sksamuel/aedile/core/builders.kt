package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.RemovalListener
import com.github.benmanes.caffeine.cache.Weigher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import java.util.function.Function
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun caffeineBuilder(): Builder<Any, Any> {
   return Builder(Caffeine.newBuilder())
}

class Builder<K, V>(private val builder: Caffeine<K, V>) {

   private var scope: CoroutineScope? = null

   fun on(scope: CoroutineScope): Builder<K, V> {
      this.scope = scope
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
    * Sets the [CoroutineDispatcher] that is used by the loading function.
    */
   fun withDispatcher(dispatcher: CoroutineDispatcher): Builder<K, V> {
      builder.executor(dispatcher.asExecutor())
      return this
   }

   /**
    * Builds a cache which does not automatically load values when keys
    * are requested unless a mapping function is provided.
    *
    * If the asynchronous computation fails or computes a null value then
    * the entry will be automatically removed.
    *
    * Note that multiple threads can concurrently load values for distinct keys.
    *
    * Consider buildAsync(CacheLoader) or buildAsync(AsyncCacheLoader) instead,
    * if it is feasible to implement an CacheLoader or AsyncCacheLoader.
    *
    */
   fun <K1 : K, V1 : V> buildAsync(): AedileAsync<K1, V1> {
      return AedileAsync(builder.buildAsync<K1, V1>())
   }

   /**
    * Builds a cache, which either returns a CompletableFuture already loaded
    * or currently computing the value for a given key, or atomically computes
    * the value asynchronously through a supplied mapping function or the supplied AsyncCacheLoader.
    *
    * If the asynchronous computation fails or computes a null value then the
    * entry will be automatically removed. Note that multiple threads can
    * concurrently load values for distinct keys.
    *
    */
   fun <K1 : K, V1 : V> buildAsync2(load: suspend (K1) -> V1): AedileAsync<K1, V1> {
      return AedileAsync(builder.buildAsync { key, _ -> scope!!.async { load(key) }.asCompletableFuture() })
   }

   /**
    * Specifies a nanosecond-precision time source for use in determining when entries
    * should be expired or refreshed. By default, System.nanoTime is used.
    */
   fun ticker(f: () -> Long): Builder<K, V> {
      builder.ticker { f() }
      return this
   }

   /**
    * Sets the minimum total size for the internal data structures.
    *
    * Providing a large enough estimate at construction time avoids the
    * need for expensive resizing operations later,
    * but setting this value unnecessarily high wastes memory.
    */
   fun initialCapacity(initialCapacity: Int): Builder<K, V> {
      builder.initialCapacity(initialCapacity)
      return this
   }

   fun evictionListener(listener: (K?, V?, RemovalCause) -> Unit): Builder<K, V> {
      builder.evictionListener(RemovalListener<K, V> { key, value, cause -> listener(key, value, cause) })
      return this
   }
}

class AedileAsync<K, V>(private val cache: AsyncCache<K, V>) {

   private var scope: CoroutineScope? = null

   suspend fun getIfPresent(key: K): V? {
      return cache.getIfPresent(key)?.await()
   }

   suspend fun getOrPut(key: K, f: (K) -> V): V? {
      return cache.get(key, Function<K, V> { f(it) })?.await()
   }

   /**
    * Associates a computed value with the given [key] in this cache.
    * If the cache previously contained a value associated with key,
    * the old value is replaced by the new value.
    *
    * If the suspendable computation throws, the entry will be automatically removed.
    */
   suspend fun foo(key: K, compute: () -> V) {
      cache.put(key, scope!!.async { compute() }.asCompletableFuture())
   }
}

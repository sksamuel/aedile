package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Weigher
import kotlinx.coroutines.future.await
import java.util.function.Function
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun caffeineBuilder(): Builder<Any, Any> {
   return Builder(Caffeine.newBuilder())
}

class Builder<K, V>(private val builder: Caffeine<K, V>) {

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

   fun withWeakKeys(): Builder<K, V> {
      builder.weakKeys()
      return this
   }

   fun withWeakValues(): Builder<K, V> {
      builder.weakValues()
      return this
   }

   fun <K1 : K, V1 : V> build(): Aedile<K1, V1> {
      return Aedile(builder.build<K1, V1>())
   }

   fun <K1 : K, V1 : V> buildAsync(): AedileAsync<K1, V1> {
      return AedileAsync(builder.buildAsync<K1, V1>())
   }
}

class Aedile<K, V>(private val cache: Cache<K, V>) {

   fun getIfPresent(key: K): V? {
      return cache.getIfPresent(key)
   }

}

class AedileAsync<K, V>(private val cache: AsyncCache<K, V>) {

   suspend fun getIfPresent(key: K): V? {
      return cache.getIfPresent(key)?.await()
   }

   suspend fun getOrPut(key: K, f: (K) -> V): V? {
      return cache.get(key, Function<K, V> { f(it) })?.await()
   }
}

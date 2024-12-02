package com.sksamuel.aedile.micrometer

import com.sksamuel.aedile.core.Cache
import com.sksamuel.aedile.core.LoadingCache
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics

@Deprecated("Use CaffeineCacheMetrics directly")
class CacheMetrics<K, V>(
   private val cache: Cache<K, V>,
   private val cacheName: String,
   private val tags: Collection<Tag> = emptyList()
) : MeterBinder {

   override fun bindTo(registry: MeterRegistry) {
      cache.underlying().synchronous().stats()
      CaffeineCacheMetrics(cache.underlying().synchronous(), cacheName, tags).bindTo(registry)
   }
}

@Deprecated("Use CaffeineCacheMetrics directly")
class LoadingCacheMetrics<K, V>(
   private val cache: LoadingCache<K, V>,
   private val cacheName: String,
   private val tags: Collection<Tag> = emptyList()
) : MeterBinder {

   override fun bindTo(registry: MeterRegistry) {
      CaffeineCacheMetrics(cache.underlying().synchronous(), cacheName, tags).bindTo(registry)
   }
}

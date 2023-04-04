package com.sksamuel.aedile.micrometer

import com.sksamuel.aedile.core.CacheFacade
import com.sksamuel.aedile.core.LoadingCacheFacade
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics

class CacheMetrics<K, V>(
   private val cache: CacheFacade<K, V>,
   private val cacheName: String,
   private val tags: Collection<Tag> = emptyList()
) : MeterBinder {

   override fun bindTo(registry: MeterRegistry) {
      cache.underlying().synchronous().stats()
      CaffeineCacheMetrics(cache.underlying().synchronous(), cacheName, tags).bindTo(registry)
   }
}

class LoadingCacheMetrics<K, V>(
   private val cache: LoadingCacheFacade<K, V>,
   private val cacheName: String,
   private val tags: Collection<Tag> = emptyList()
) : MeterBinder {

   override fun bindTo(registry: MeterRegistry) {
      CaffeineCacheMetrics(cache.underlying().synchronous(), cacheName, tags).bindTo(registry)
   }
}

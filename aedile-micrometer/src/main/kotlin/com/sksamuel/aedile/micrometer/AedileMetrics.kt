package com.sksamuel.aedile.micrometer

import com.sksamuel.aedile.core.Cache
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics

class AedileMetrics<K, V>(
   private val cache: Cache<K, V>,
   private val cacheName: String,
   private val tags: Collection<Tag> = emptyList()
) : MeterBinder {

   override fun bindTo(registry: MeterRegistry) {
      CaffeineCacheMetrics(cache.underlying().synchronous(), cacheName, tags).bindTo(registry)
   }
}

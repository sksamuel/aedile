package com.sksamuel.aedile.micrometer

import com.sksamuel.aedile.core.Cache
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics

class AedileMetrics<K, V>(private val cache: Cache<K, V>, private val cacheName: String) : MeterBinder {

   override fun bindTo(registry: MeterRegistry) {
      val c: com.github.benmanes.caffeine.cache.Cache<K, V> = cache.underlying().synchronous()
      CaffeineCacheMetrics<K, V, com.github.benmanes.caffeine.cache.Cache<K, V>>(c, cacheName, emptyList()).bindTo(registry)
   }
}

package com.sksamuel.aedile.core

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Weigher
import kotlin.time.Duration
import kotlin.time.toJavaDuration

fun <K, V> builder(): Cache<K, V> {
   return Builder(Caffeine.newBuilder())
}

class Builder<K, V>(private val builder: Caffeine<Any, Any>) {
   fun expireAfterAccess(duration: Duration): Builder {
      builder.expireAfterAccess(duration.toJavaDuration())
      return this
   }

   fun expireAfterWrite(duration: Duration): Builder {
      builder.expireAfterWrite(duration.toJavaDuration())
      return this
   }

   fun maximumWeight(maximumWeight: Long): Builder {
      builder.maximumWeight(maximumWeight)
      return this
   }

   fun <K, V> maximumWeight(weigher): Builder {
      builder.weigher(object : Weigher<K, V> {})
      return this
   }
}

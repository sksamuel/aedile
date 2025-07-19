package com.sksamuel.aedile.core

import kotlinx.coroutines.Deferred
import kotlin.time.Duration

fun interface Scheduler {
   fun schedule(command: () -> Unit, duration: Duration): Deferred<Unit>
}



package com.sksamuel.aedile.core

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.CoroutineContext

val booleanThreadLocal: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

class BooleanThreadContextElement(private val value: Boolean): ThreadContextElement<Boolean> {
    companion object Key : CoroutineContext.Key<BooleanThreadContextElement>
    override val key : CoroutineContext.Key<BooleanThreadContextElement> = Key

    override fun updateThreadContext(context: CoroutineContext): Boolean {
        return booleanThreadLocal.get().also {
            booleanThreadLocal.set(value)
        }
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Boolean) {
        booleanThreadLocal.set(oldState)
    }
}
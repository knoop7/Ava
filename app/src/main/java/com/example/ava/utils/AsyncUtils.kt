package com.example.ava.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun AsynchronousServerSocketChannel.acceptAsync(): AsynchronousSocketChannel =
    suspendCancellableCoroutine { cont ->
        accept(cont, asyncIOHandler())
    }

@Suppress("UNCHECKED_CAST")
private fun <T> asyncIOHandler(): CompletionHandler<T, CancellableContinuation<T>> =
    AsyncIOHandlerAny as CompletionHandler<T, CancellableContinuation<T>>

private object AsyncIOHandlerAny : CompletionHandler<Any, CancellableContinuation<Any>> {
    override fun completed(result: Any, cont: CancellableContinuation<Any>) {
        cont.resume(result)
    }

    override fun failed(ex: Throwable, cont: CancellableContinuation<Any>) {
        
        if (ex is AsynchronousCloseException && cont.isCancelled) return
        cont.resumeWithException(ex)
    }
}
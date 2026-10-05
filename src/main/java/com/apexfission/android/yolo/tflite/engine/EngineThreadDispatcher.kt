package com.apexfission.android.yolo.tflite.engine

import java.io.Closeable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask

/**
 * Owns one physical worker thread. Share this owner with pipeline wrappers, then close those
 * wrappers before closing the owner. Arbitrary coroutine dispatchers/executors are not accepted.
 * Nested operations already on the worker execute inline, avoiding single-thread deadlocks.
 */
class EngineThreadDispatcher : Closeable {
    @Volatile
    private var worker: Thread? = null
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SingleThreadEngineWorker").also { worker = it }
    }
    val isShutdown: Boolean get() = executor.isShutdown
    internal val isWorkerThread: Boolean get() = Thread.currentThread() === worker

    internal fun execute(task: FutureTask<*>) {
        try {
            if (isWorkerThread) task.run() else executor.execute(task)
        } catch (failure: java.util.concurrent.RejectedExecutionException) {
            task.cancel(false)
            throw failure
        }
    }

    internal fun <T> call(block: () -> T): T {
        val task = FutureTask(java.util.concurrent.Callable { block() })
        execute(task)
        return task.awaitCompletion()
    }

    override fun close() {
        executor.shutdown()
    }
}

/** A synchronous caller retains ownership until native work completes, even on interruption. */
internal fun <T> Future<T>.awaitCompletion(): T {
    var interrupted = false
    try {
        while (true) {
            try {
                return get()
            } catch (_: InterruptedException) {
                interrupted = true
            } catch (failure: ExecutionException) {
                throw (failure.cause ?: failure)
            }
        }
    } finally {
        if (interrupted) Thread.currentThread().interrupt()
    }
}

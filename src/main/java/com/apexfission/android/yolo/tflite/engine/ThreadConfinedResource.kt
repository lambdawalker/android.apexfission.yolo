package com.apexfission.android.yolo.tflite.engine

import java.io.Closeable
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

/** Shared admission/ownership protocol used by all pipeline wrappers. */
class ThreadConfinedResource<T : Closeable>(
    sharedContext: EngineThreadDispatcher? = null,
    factory: () -> T,
) : Closeable {
    private val context = sharedContext ?: EngineThreadDispatcher()
    private val ownsContext = sharedContext == null
    private val admission = Any()
    private var closeTask: FutureTask<Unit>? = null

    // Accessed only on the worker; nested inline close may overtake queued calls.
    private var disposed = false
    @Volatile
    var closeThreadId: Long = -1L
        private set
    val value: T = try {
        context.call(factory)
    } catch (failure: Throwable) {
        if (ownsContext) context.close()
        throw failure
    }

    fun <R> call(ifClosed: () -> R, operation: (T) -> R): R {
        val task = synchronized(admission) {
            if (closeTask != null) return ifClosed()
            FutureTask(Callable { if (disposed) ifClosed() else operation(value) }).also { context.execute(it) }
        }
        return task.awaitCompletion()
    }

    override fun close() {
        val task = synchronized(admission) {
            closeTask?.let {
                // A recursive close from native teardown must not wait for itself.
                if (context.isWorkerThread) return
                return@synchronized it
            }
            FutureTask(Callable {
                disposed = true
                closeThreadId = Thread.currentThread().id
                try {
                    value.close()
                } finally {
                    if (ownsContext) context.close()
                }
            }).also {
                closeTask = it
                context.execute(it)
            }
        }
        task.awaitCompletion()
    }
}

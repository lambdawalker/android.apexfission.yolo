package com.apexfission.android.yolo.tflite.engine

import java.io.Closeable
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class ThreadConfinedResourceTest {
    private class Resource(val closeAction: () -> Unit = {}) : Closeable {
        override fun close() = closeAction()
    }

    @Test(timeout = 10000)
    fun inlineCloseRejectsPreviouslyQueuedUse() {
        EngineThreadDispatcher().use { context ->
            val resource = ThreadConfinedResource(context) { Resource() }
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val closing = FutureTask(Callable {
                entered.countDown()
                check(release.await(3, TimeUnit.SECONDS))
                resource.close()
            })
            context.execute(closing)
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            val result = FutureTask(Callable {
                resource.call({ -1 }) { fail("Queued operation used disposed resource"); 42 }
            })
            val caller = Thread(result)
            try {
                caller.start()
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
                while (caller.state != Thread.State.WAITING && System.nanoTime() < deadline) {
                    Thread.yield()
                }
                assertEquals(Thread.State.WAITING, caller.state)
                release.countDown()
                closing.get(3, TimeUnit.SECONDS)
                assertEquals(-1, result.get(3, TimeUnit.SECONDS))
            } finally {
                release.countDown()
                caller.join(3000)
                resource.close()
            }
        }
    }

    @Test(timeout = 10000)
    fun repeatedCloseAfterOwnerShutdownDoesNotHang() {
        val context = EngineThreadDispatcher()
        val resource = ThreadConfinedResource(context) { Resource() }
        context.close()
        try {
            resource.close()
            fail("Closed owner must reject teardown")
        } catch (_: RejectedExecutionException) { }
        try {
            resource.close()
            fail("Unsubmitted teardown must be cancelled")
        } catch (_: CancellationException) { }
    }

    @Test(timeout = 10000)
    fun nestedConstructionCallsAndCloseShareOneThread() {
        val context = EngineThreadDispatcher()
        var child: ThreadConfinedResource<Resource>? = null
        var constructionThread: Thread? = null
        var closeThread: Thread? = null
        val parent = ThreadConfinedResource(context) {
            constructionThread = Thread.currentThread()
            child = ThreadConfinedResource(context) { Resource { closeThread = Thread.currentThread() } }
            Resource { child!!.close() }
        }
        try {
            val thread = parent.call({ error("closed") }) {
                child!!.call({ error("closed") }) { Thread.currentThread() }
            }
            assertSame(constructionThread, thread)
            parent.close()
            assertSame(constructionThread, closeThread)
        } finally {
            parent.close()
            child?.close()
            context.close()
        }
    }

    @Test(timeout = 10000)
    fun failedOwnedFactoryTerminatesWorker() {
        var worker: Thread? = null
        val failure = IllegalArgumentException("factory")
        try {
            ThreadConfinedResource<Resource> {
                worker = Thread.currentThread()
                throw failure
            }
            fail("Factory must fail")
        } catch (actual: IllegalArgumentException) {
            assertSame(failure, actual)
        }
        worker!!.join(3000)
        assertFalse(worker!!.isAlive)
    }

    @Test(timeout = 10000)
    fun failedBorrowedFactoryDoesNotCloseContext() {
        EngineThreadDispatcher().use { context ->
            try {
                ThreadConfinedResource<Resource>(context) { error("factory") }
                fail("Factory must fail")
            } catch (_: IllegalStateException) { }
            ThreadConfinedResource(context) { Resource() }.use { resource ->
                assertEquals(42, resource.call({ -1 }) { 42 })
            }
            assertFalse(context.isShutdown)
        }
    }

    @Test(timeout = 10000)
    fun interruptedCallerWaitsForResourceUseToFinish() {
        val resource = ThreadConfinedResource { Resource() }
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val callers = Executors.newSingleThreadExecutor()
        var caller: Thread? = null
        try {
            val result = callers.submit(Callable {
                caller = Thread.currentThread()
                resource.call({ error("closed") }) {
                    entered.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                }
                try {
                    assertTrue(Thread.currentThread().isInterrupted)
                    finished.countDown()
                } finally { Thread.interrupted() }
            })
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            caller!!.interrupt()
            assertFalse(finished.await(100, TimeUnit.MILLISECONDS))
            release.countDown()
            result.get(3, TimeUnit.SECONDS)
        } finally {
            release.countDown()
            resource.close()
            callers.shutdownNow()
        }
    }

    @Test(timeout = 10000)
    fun concurrentCloseIsExactlyOnceAndRejectsLaterWork() {
        val closes = AtomicInteger()
        val resource = ThreadConfinedResource { Resource { closes.incrementAndGet() } }
        val callers = Executors.newFixedThreadPool(6)
        val start = CountDownLatch(1)
        try {
            val results = List(6) { index -> callers.submit(Callable {
                check(start.await(3, TimeUnit.SECONDS))
                if (index % 2 == 0) resource.close()
                else resource.call({ -1 }) { 42 }
            }) }
            start.countDown()
            results.forEach { it.get(3, TimeUnit.SECONDS) }
            assertEquals(1, closes.get())
            assertEquals(-1, resource.call({ -1 }) { fail("Closed resource used"); 42 })
        } finally {
            start.countDown()
            resource.close()
            callers.shutdownNow()
        }
    }
}

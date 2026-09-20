package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutures
import com.google.api.core.SettableApiFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiFutureTest {
    @Test
    fun `returns an already completed result`() = runBlocking {
        assertEquals("Result", ApiFutures.immediateFuture("Result").await())
    }

    @Test
    fun `supports nullable results`() = runBlocking {
        assertNull(ApiFutures.immediateFuture<String?>(null).await())
    }

    @Test
    fun `suspends without blocking until the future completes`() = runBlocking {
        val future = SettableApiFuture.create<String>()
        val result = async(start = CoroutineStart.UNDISPATCHED) { future.await() }

        assertFalse(result.isCompleted)
        future.set("Result")

        assertEquals("Result", result.await())
        assertFalse(future.isCancelled)
    }

    @Test
    fun `propagates an already failed future without an ExecutionException wrapper`() = runBlocking {
        val failure = IOException("Firestore unavailable")

        val thrown = assertFailsWith<IOException> {
            ApiFutures.immediateFailedFuture<String>(failure).await()
        }

        assertEquals(failure.message, thrown.message)
    }

    @Test
    fun `propagates failure after suspending`() = runBlocking {
        supervisorScope {
            val future = SettableApiFuture.create<String>()
            val failure = IOException("Firestore unavailable")
            val result = async(start = CoroutineStart.UNDISPATCHED) { future.await() }

            assertFalse(result.isCompleted)
            future.setException(failure)

            val thrown = assertFailsWith<IOException> { result.await() }
            assertEquals(failure.message, thrown.message)
        }
    }

    @Test
    fun `propagates an already cancelled future`(): Unit = runBlocking {
        assertFailsWith<CancellationException> {
            ApiFutures.immediateCancelledFuture<String>().await()
        }
    }

    @Test
    fun `cancelling the future cancels the suspended awaiter`() = runBlocking {
        val future = SettableApiFuture.create<String>()
        val result = async(start = CoroutineStart.UNDISPATCHED) { future.await() }

        future.cancel(false)

        assertFailsWith<CancellationException> { result.await() }
        assertTrue(result.isCancelled)
    }

    @Test
    fun `cancelling the coroutine cancels the future without interrupting`() = runBlocking {
        val delegate = SettableApiFuture.create<String>()
        var interruptRequested: Boolean? = null
        val future = object : ApiFuture<String> by delegate {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                interruptRequested = mayInterruptIfRunning
                return delegate.cancel(mayInterruptIfRunning)
            }
        }
        val result = async(start = CoroutineStart.UNDISPATCHED) { future.await() }

        result.cancelAndJoin()

        assertTrue(future.isCancelled)
        assertEquals(false, interruptRequested)
    }

    @Test
    fun `cancellation wins when completion is awaiting coroutine dispatch`() = runBlocking {
        val future = SettableApiFuture.create<String>()
        val result = async(start = CoroutineStart.UNDISPATCHED) { future.await() }

        future.set("Result")
        result.cancel()

        assertFailsWith<CancellationException> { result.await() }
        assertTrue(result.isCancelled)
        assertFalse(future.isCancelled)
    }

    @Test
    fun `stops waiting even when the future refuses cancellation`() = runBlocking {
        val delegate = SettableApiFuture.create<String>()
        val future = object : ApiFuture<String> by delegate {
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        }
        val result = async(start = CoroutineStart.UNDISPATCHED) { future.await() }

        result.cancelAndJoin()

        assertTrue(result.isCancelled)
        assertFalse(future.isDone)
        assertTrue(delegate.set("Late result"))
        assertTrue(result.isCancelled)
    }
}

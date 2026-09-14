package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> ApiFuture<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        ApiFutures.addCallback(
            this,
            object : ApiFutureCallback<T> {
                override fun onSuccess(result: T) {
                    continuation.resume(result)
                }

                override fun onFailure(t: Throwable) {
                    if (t is CancellationException) {
                        continuation.cancel(t)
                    } else {
                        continuation.resumeWithException(t)
                    }
                }
            },
        ) { it.run() }

        continuation.invokeOnCancellation {
            cancel(false)
        }
    }
}

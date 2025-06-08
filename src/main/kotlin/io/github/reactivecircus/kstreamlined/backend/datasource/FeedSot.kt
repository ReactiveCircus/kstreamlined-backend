package io.github.reactivecircus.kstreamlined.backend.datasource

suspend inline fun <T : Any> feedSot(
    localSource: () -> List<T>?,
    persistToLocal: (List<T>) -> Unit,
    remoteSource: suspend () -> List<T>,
): List<T> {
    persistToLocal(remoteSource())
    return localSource() ?: error("Local source returned null.")
}

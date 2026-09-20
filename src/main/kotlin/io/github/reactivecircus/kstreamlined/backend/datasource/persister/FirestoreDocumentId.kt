package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinItem
import kotlin.text.substringAfterLast

internal val String.firestoreDocumentId: String
    get() = substringAfterLast("=")

internal val KotlinBlogItem.firestoreDocumentId: String
    get() = guid.firestoreDocumentId

internal val KotlinYouTubeItem.firestoreDocumentId: String
    get() = id.firestoreDocumentId

internal val TalkingKotlinItem.firestoreDocumentId: String
    get() = guid.replace("/", "-")

internal val KotlinWeeklyItem.firestoreDocumentId: String
    get() = guid.substringAfterLast("/")

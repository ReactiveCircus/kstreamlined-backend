package io.github.reactivecircus.kstreamlined.backend.store

import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.service.dto.TalkingKotlinItem
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

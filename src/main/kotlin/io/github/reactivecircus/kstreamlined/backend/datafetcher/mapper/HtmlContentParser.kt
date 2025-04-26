package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document

fun tryParseHtml(text: String): Document? = Ksoup.parse(text).let { doc ->
    if (doc.body().childrenSize() == 0) null else doc
}

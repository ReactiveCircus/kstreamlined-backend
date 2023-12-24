package io.github.reactivecircus.kstreamlined.backend.client.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement

object Namespace {
    const val atom = "http://www.w3.org/2005/Atom"
    const val media = "http://search.yahoo.com/mrss/"
    const val content = "http://purl.org/rss/1.0/modules/content/"
    const val yt = "http://www.youtube.com/xml/schemas/2015"
    const val itunes = "http://www.itunes.com/dtds/podcast-1.0.dtd"
}

@Serializable
data class Link(
    @XmlElement(false)
    val href: String,
    @XmlElement(false)
    val rel: String,
    @XmlElement(false)
    val type: String? = null,
    @XmlElement(false)
    val title: String? = null,
)

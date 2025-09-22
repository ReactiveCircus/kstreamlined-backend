package io.github.reactivecircus.kstreamlined.backend.datasource.dto

import io.github.reactivecircus.kstreamlined.backend.NoArg
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement

object Namespace {
    const val Atom = "http://www.w3.org/2005/Atom"
    const val Aedia = "http://search.yahoo.com/mrss/"
    const val Content = "http://purl.org/rss/1.0/modules/content/"
    const val Yt = "http://www.youtube.com/xml/schemas/2015"
    const val Itunes = "http://www.itunes.com/dtds/podcast-1.0.dtd"
}

@NoArg
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

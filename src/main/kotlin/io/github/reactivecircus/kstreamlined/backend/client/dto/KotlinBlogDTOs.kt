package io.github.reactivecircus.kstreamlined.backend.client.dto.kotlinblog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("rss", "", "")
@Serializable
data class KotlinBlogRss(
    @XmlElement(false)
    val version: String,
    val channel: KotlinBlogChannel,
)

@XmlSerialName("channel", "", "")
@Serializable
data class KotlinBlogChannel(
    @XmlElement(true)
    val title: String,
    @XmlSerialName(value = "link", namespace = Namespace.atom, prefix = "")
    val atomLink: AtomLink,
    @XmlElement(true)
    val link: String,
    @XmlElement(true)
    val description: String,
    @XmlElement(true)
    val lastBuildDate: String,
    @XmlElement(true)
    val language: String,
    @XmlElement(true)
    @XmlSerialName(value = "updatePeriod", namespace = Namespace.sy, prefix = "")
    val updatePeriod: String,
    @XmlElement(true)
    @XmlSerialName(value = "updateFrequency", namespace = Namespace.sy, prefix = "")
    val updateFrequency: String,
    val image: Image,
    val items: List<KotlinBlogItem>,
) {
    @Serializable
    data class AtomLink(
        @XmlElement(false)
        val href: String,
        @XmlElement(false)
        val rel: String,
        @XmlElement(false)
        val type: String,
    )

    @XmlSerialName("image", "", "")
    @Serializable
    data class Image(
        @XmlElement(true)
        val url: String,
        @XmlElement(true)
        val title: String,
        @XmlElement(true)
        val link: String,
        @XmlElement(true)
        val width: String,
        @XmlElement(true)
        val height: String,
    )
}

@XmlSerialName("item", "", "")
@Serializable
data class KotlinBlogItem(
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val link: String,
    @XmlElement(true)
    @XmlSerialName(value = "creator", namespace = Namespace.dc, prefix = "")
    val creator: String,
    @XmlElement(true)
    val pubDate: String,
    @XmlElement(true)
    val featuredImage: String,
    @XmlElement(true)
    @SerialName("category")
    val categories: List<String>,
    @XmlElement(true)
    val guid: String,
    @XmlElement(true)
    val description: String,
    @XmlElement(true)
    @XmlSerialName(value = "encoded", namespace = Namespace.content, prefix = "")
    val encoded: String,
    @XmlElement(true)
    val languages: List<Language>,
) {
    @XmlSerialName("language", "", "")
    @Serializable
    data class Language(
        @XmlElement(true)
        val code: String,
        @XmlElement(true)
        val url: String,
    )
}

object Namespace {
    const val content = "http://purl.org/rss/1.0/modules/content/"
    const val dc = "http://purl.org/dc/elements/1.1/"
    const val atom = "http://www.w3.org/2005/Atom"
    const val sy = "http://purl.org/rss/1.0/modules/syndication/"
}

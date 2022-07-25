package io.github.reactivecircus.kstreamlined.backend.client.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("rss", "", "")
@Serializable
data class KotlinWeeklyRss(
    @XmlElement(false)
    val version: String,
    val channel: KotlinWeeklyChannel,
)

@XmlSerialName("channel", "", "")
@Serializable
data class KotlinWeeklyChannel(
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val description: String,
    @XmlElement(true)
    val link: String,
    val image: Image,
    @XmlElement(true)
    val generator: String,
    @XmlElement(true)
    val lastBuildDate: String,
    @XmlSerialName(value = "link", namespace = Namespace.atom, prefix = "")
    val atomLink: Link,
    @XmlElement(true)
    val language: String,
    val items: List<KotlinWeeklyItem>,
) {
    @XmlSerialName("image", "", "")
    @Serializable
    data class Image(
        @XmlElement(true)
        val url: String,
        @XmlElement(true)
        val title: String,
        @XmlElement(true)
        val link: String,
    )
}

@XmlSerialName("item", "", "")
@Serializable
data class KotlinWeeklyItem(
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val description: String,
    @XmlElement(true)
    val link: String,
    @XmlElement(true)
    val guid: String,
    @XmlElement(true)
    @XmlSerialName(value = "creator", namespace = Namespace.dc, prefix = "")
    val creator: String,
    @XmlElement(true)
    val pubDate: String,
)

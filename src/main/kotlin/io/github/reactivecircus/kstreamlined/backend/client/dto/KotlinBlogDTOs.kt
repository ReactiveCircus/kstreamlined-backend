package io.github.reactivecircus.kstreamlined.backend.client.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("rss", "", "")
@Serializable
data class KotlinBlogRss(
    val channel: KotlinBlogChannel,
)

@XmlSerialName("channel", "", "")
@Serializable
data class KotlinBlogChannel(
    val items: List<KotlinBlogItem>,
)

@XmlSerialName("item", "", "")
@Serializable
data class KotlinBlogItem(
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val link: String,
    @XmlElement(true)
    val pubDate: String,
    @XmlElement(true)
    val featuredImage: String?,
    @XmlElement(true)
    val guid: String,
    @XmlElement(true)
    val description: String,
)

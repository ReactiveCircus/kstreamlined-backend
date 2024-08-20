package io.github.reactivecircus.kstreamlined.backend.datasource.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("feed", Namespace.atom, "")
@Serializable
data class KotlinYouTubeRss(
    @XmlSerialName("link", Namespace.atom, "")
    val links: List<Link>,
    @XmlElement(true)
    @XmlSerialName(value = "id", namespace = Namespace.atom, prefix = "")
    val id: String,
    @XmlElement(true)
    @XmlSerialName(value = "channelId", namespace = Namespace.yt, prefix = "")
    val channelId: String,
    @XmlElement(true)
    @XmlSerialName(value = "title", namespace = Namespace.atom, prefix = "")
    val title: String,
    val author: KotlinYouTubeAuthor,
    @XmlElement(true)
    @XmlSerialName(value = "published", namespace = Namespace.atom, prefix = "")
    val published: String,
    val entries: List<KotlinYouTubeItem>,
)

@XmlSerialName("author", Namespace.atom, "")
@Serializable
data class KotlinYouTubeAuthor(
    @XmlElement(true)
    val name: String,
    @XmlElement(true)
    val uri: String,
)

@XmlSerialName("group", Namespace.media, "")
@Serializable
data class MediaGroup(
    @XmlElement(true)
    @XmlSerialName(value = "title", namespace = Namespace.media, prefix = "")
    val title: String,
    val content: Content,
    val thumbnail: Thumbnail,
    @XmlElement(true)
    @XmlSerialName(value = "description", namespace = Namespace.media, prefix = "")
    val description: String,
    val community: MediaCommunity,
) {
    @XmlSerialName("content", Namespace.media, "")
    @Serializable
    data class Content(
        @XmlElement(false)
        val url: String,
        @XmlElement(false)
        val type: String,
        @XmlElement(false)
        val width: String,
        @XmlElement(false)
        val height: String,
    )

    @XmlSerialName("thumbnail", Namespace.media, "")
    @Serializable
    data class Thumbnail(
        @XmlElement(false)
        val url: String,
        @XmlElement(false)
        val width: String,
        @XmlElement(false)
        val height: String,
    )
}

@XmlSerialName("community", Namespace.media, "")
@Serializable
data class MediaCommunity(
    val starRating: StarRating,
    val statistics: Statistics,
) {
    @XmlSerialName("starRating", Namespace.media, "")
    @Serializable
    data class StarRating(
        @XmlElement(false)
        val count: String,
        @XmlElement(false)
        val average: String,
        @XmlElement(false)
        val min: String,
        @XmlElement(false)
        val max: String,
    )

    @XmlSerialName("statistics", Namespace.media, "")
    @Serializable
    data class Statistics(
        @XmlElement(false)
        val views: String
    )
}

@XmlSerialName("entry", Namespace.atom, "")
@Serializable
data class KotlinYouTubeItem(
    @XmlElement(true)
    val id: String,
    @XmlElement(true)
    @XmlSerialName(value = "videoId", namespace = Namespace.yt, prefix = "")
    val videoId: String,
    @XmlElement(true)
    @XmlSerialName(value = "channelId", namespace = Namespace.yt, prefix = "")
    val channelId: String,
    @XmlElement(true)
    val title: String,
    @XmlSerialName(value = "link", namespace = Namespace.atom, prefix = "")
    val link: Link,
    val author: KotlinYouTubeAuthor,
    @XmlElement(true)
    val published: String,
    @XmlElement(true)
    val updated: String,
    val mediaGroup: MediaGroup,
)

package io.github.reactivecircus.kstreamlined.backend.client.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("feed", KotlinYouTubeRss.Namespace.atom, "")
@Serializable
data class KotlinYouTubeRss(
    @XmlElement(true)
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
    val author: Author,
    @XmlElement(true)
    @XmlSerialName(value = "published", namespace = Namespace.atom, prefix = "")
    val published: String,
    @XmlElement(true)
    val entries: List<KotlinYouTubeItem>,
) {
    object Namespace {
        const val atom = "http://www.w3.org/2005/Atom"
        const val yt = "http://www.youtube.com/xml/schemas/2015"
        const val media = "http://search.yahoo.com/mrss/"
    }
}

@XmlSerialName("link", KotlinYouTubeRss.Namespace.atom, "")
@Serializable
data class Link(
    @XmlElement(false)
    val href: String,
    @XmlElement(false)
    val rel: String,
)

@XmlSerialName("author", KotlinYouTubeRss.Namespace.atom, "")
@Serializable
data class Author(
    @XmlElement(true)
    val name: String,
    @XmlElement(true)
    val uri: String,
)

@XmlSerialName("group", KotlinYouTubeRss.Namespace.media, "")
@Serializable
data class MediaGroup(
    @XmlElement(true)
    @XmlSerialName(value = "title", namespace = KotlinYouTubeRss.Namespace.media, prefix = "")
    val title: String,
    val content: Content,
    val thumbnail: Thumbnail,
    @XmlElement(true)
    @XmlSerialName(value = "description", namespace = KotlinYouTubeRss.Namespace.media, prefix = "")
    val description: String,
    val community: MediaCommunity,
) {
    @XmlSerialName("content", KotlinYouTubeRss.Namespace.media, "")
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

    @XmlSerialName("thumbnail", KotlinYouTubeRss.Namespace.media, "")
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

@XmlSerialName("community", KotlinYouTubeRss.Namespace.media, "")
@Serializable
data class MediaCommunity(
    val starRating: StarRating,
    val statistics: Statistics,
) {
    @XmlSerialName("starRating", KotlinYouTubeRss.Namespace.media, "")
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

    @XmlSerialName("statistics", KotlinYouTubeRss.Namespace.media, "")
    @Serializable
    data class Statistics(
        @XmlElement(false)
        val views: String
    )
}

@XmlSerialName("entry", KotlinYouTubeRss.Namespace.atom, "")
@Serializable
data class KotlinYouTubeItem(
    @XmlElement(true)
    val id: String,
    @XmlElement(true)
    @XmlSerialName(value = "videoId", namespace = KotlinYouTubeRss.Namespace.yt, prefix = "")
    val videoId: String,
    @XmlElement(true)
    @XmlSerialName(value = "channelId", namespace = KotlinYouTubeRss.Namespace.yt, prefix = "")
    val channelId: String,
    @XmlElement(true)
    val title: String,
    val link: Link,
    val author: Author,
    @XmlElement(true)
    val published: String,
    @XmlElement(true)
    val updated: String,
    val mediaGroup: MediaGroup,
)

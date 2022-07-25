package io.github.reactivecircus.kstreamlined.backend.client.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("feed", Namespace.atom, "")
@Serializable
data class TalkingKotlinRss(
    @XmlElement(true)
    @XmlSerialName(value = "generator", namespace = Namespace.atom, prefix = "")
    val generator: String,
    @XmlSerialName("link", Namespace.atom, "")
    val links: List<Link>,
    @XmlElement(true)
    @XmlSerialName(value = "updated", namespace = Namespace.atom, prefix = "")
    val updated: String,
    @XmlElement(true)
    @XmlSerialName(value = "id", namespace = Namespace.atom, prefix = "")
    val id: String,
    @XmlElement(true)
    @XmlSerialName(value = "title", namespace = Namespace.atom, prefix = "")
    val title: String,
    @XmlElement(true)
    @XmlSerialName(value = "subtitle", namespace = Namespace.atom, prefix = "")
    val subtitle: String,
    val entries: List<TalkingKotlinItem>,
)

@XmlSerialName("entry", Namespace.atom, "")
@Serializable
data class TalkingKotlinItem(
    @XmlElement(true)
    val id: String,
    @XmlElement(true)
    val title: String,
    @XmlSerialName("link", Namespace.atom, "")
    val link: Link,
    val author: Author,
    @XmlElement(true)
    val published: String,
    @XmlElement(true)
    val updated: String,
    val content: Content,
    val categories: List<Category>,
    val summary: Summary,
    val thumbnail: Thumbnail,
    val mediaContent: MediaContent,
) {
    @XmlSerialName("author", Namespace.atom, "")
    @Serializable
    data class Author(
        @XmlElement(true)
        val name: String
    )

    @XmlSerialName("content", Namespace.atom, "")
    @Serializable
    data class Content(
        @XmlElement(false)
        val type: String,
        @XmlElement(false)
        @XmlSerialName("base", "http://www.w3.org/XML/1998/namespace", "")
        val base: String,
    )

    @XmlSerialName("category", Namespace.atom, "")
    @Serializable
    data class Category(
        @XmlElement(false)
        val term: String
    )

    @XmlSerialName("summary", Namespace.atom, "")
    @Serializable
    data class Summary(
        @XmlElement(false)
        val type: String
    )

    @XmlSerialName("thumbnail", Namespace.media, "")
    @Serializable
    data class Thumbnail(
        @XmlElement(false)
        val url: String
    )

    @XmlSerialName("content", Namespace.media, "")
    @Serializable
    data class MediaContent(
        @XmlElement(false)
        val medium: String,
        @XmlElement(false)
        val url: String,
    )
}

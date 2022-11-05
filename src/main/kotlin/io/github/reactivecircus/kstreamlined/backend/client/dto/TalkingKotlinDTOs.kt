package io.github.reactivecircus.kstreamlined.backend.client.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("feed", Namespace.atom, "")
@Serializable
data class TalkingKotlinRss(
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
    @XmlElement(true)
    val published: String,
    val categories: List<Category>,
) {
    @XmlSerialName("category", Namespace.atom, "")
    @Serializable
    data class Category(
        @XmlElement(false)
        val term: String
    )
}

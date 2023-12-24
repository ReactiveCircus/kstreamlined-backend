package io.github.reactivecircus.kstreamlined.backend.client.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("rss", "", "")
@Serializable
data class TalkingKotlinRss(
    val channel: TalkingKotlinChannel,
)

@XmlSerialName("channel", "", "")
@Serializable
data class TalkingKotlinChannel(
    val items: List<TalkingKotlinItem>,
)

@XmlSerialName("item", "", "")
@Serializable
data class TalkingKotlinItem(
    @XmlElement(true)
    val guid: String,
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val pubDate: String,
    @XmlElement(true)
    val link: String,
    @XmlElement(true)
    @XmlSerialName(value = "duration", namespace = Namespace.itunes, prefix = "")
    val duration: String,
    @XmlElement(true)
    @XmlSerialName(value = "summary", namespace = Namespace.itunes, prefix = "")
    val summary: String,
    val image: Image,
) {
    @XmlSerialName("image", Namespace.itunes, "")
    @Serializable
    data class Image(
        @XmlElement(false)
        val href: String,
    )
}

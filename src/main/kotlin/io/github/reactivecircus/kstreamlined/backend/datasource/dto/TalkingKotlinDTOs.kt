package io.github.reactivecircus.kstreamlined.backend.datasource.dto

import io.github.reactivecircus.kstreamlined.backend.NoArg
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

@NoArg
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
    @XmlSerialName(value = "duration", namespace = Namespace.Itunes, prefix = "")
    val duration: String,
    @XmlElement(true)
    @XmlSerialName(value = "summary", namespace = Namespace.Itunes, prefix = "")
    val summary: String,
    val enclosure: Enclosure,
    val image: Image,
) {
    @NoArg
    @XmlSerialName("enclosure", "", "")
    @Serializable
    data class Enclosure(
        @XmlElement(false)
        val url: String,
    )

    @NoArg
    @XmlSerialName("image", Namespace.Itunes, "")
    @Serializable
    data class Image(
        @XmlElement(false)
        val href: String,
    )
}

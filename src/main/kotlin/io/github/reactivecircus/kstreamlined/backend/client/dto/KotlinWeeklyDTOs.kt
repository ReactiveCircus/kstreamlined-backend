package io.github.reactivecircus.kstreamlined.backend.client.dto

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@XmlSerialName("rss", "", "")
@Serializable
data class KotlinWeeklyRss(
    val channel: KotlinWeeklyChannel,
)

@XmlSerialName("channel", "", "")
@Serializable
data class KotlinWeeklyChannel(
    val items: List<KotlinWeeklyItem>,
)

@XmlSerialName("item", "", "")
@Serializable
data class KotlinWeeklyItem(
    @XmlElement(true)
    val title: String,
    @XmlElement(true)
    val link: String,
    @XmlElement(true)
    val guid: String,
    @XmlElement(true)
    val pubDate: String,
)

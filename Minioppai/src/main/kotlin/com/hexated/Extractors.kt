package com.hexated

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixTitle
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.fixUrl
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.getQualityFromName

class Paistream : Streampai() {
    override val name = "Paistream"
    override val mainUrl = "https://paistream.my.id"
}

class TvMinioppai : Streampai() {
    override val name = "Minioppai"
    override val mainUrl = "https://tv.minioppai.org"
}

open class Streampai : ExtractorApi() {
    override val name = "Streampai"
    override val mainUrl = "https://streampai.my.id"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url, referer = referer).text
        val data = getAndUnpack(res)

        val sources = data.substringAfter("sources:[").substringBefore("]").replace("\'", "\"")
        val tracks = data.substringAfter("\"tracks\":[").substringBefore("]")

        tryParseJson<List<Responses>>("[$sources]")?.forEach {
            callback.invoke(
                ExtractorLink(
                    this.name,
                    this.name,
                    fixUrl(it.file),
                    url,
                    getQualityFromName(it.label),
                    headers = mapOf(
                        "Range" to "bytes=0-",
                    )
                )
            )
        }

        tryParseJson<List<Responses>>("[$tracks]")?.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    fixTitle(it.label ?: ""),
                    fixUrl(it.file),
                )
            )
        }
    }

    data class Responses(
        @JsonProperty("file") val file: String,
        @JsonProperty("type") val type: String?,
        @JsonProperty("label") val label: String?
    )

}
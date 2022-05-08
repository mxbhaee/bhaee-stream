package bhaee.stream.com.extractors

import com.fasterxml.jackson.annotation.JsonProperty
import bhaee.stream.com.app
import bhaee.stream.com.utils.AppUtils.parseJson
import bhaee.stream.com.utils.ExtractorApi
import bhaee.stream.com.utils.ExtractorLink
import bhaee.stream.com.utils.Qualities

class Cinestart: Tomatomatela() {
    override var name = "Cinestart"
    override var mainUrl = "https://cinestart.net"
    override val details = "vr.php?v="
}

open class Tomatomatela : ExtractorApi() {
    override var name = "Tomatomatela"
    override var mainUrl = "https://tomatomatela.com"
    override val requiresReferer = false
    private data class Tomato (
        @JsonProperty("status") val status: Int,
        @JsonProperty("file") val file: String
    )
    open val details = "details.php?v="
    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val link = url.replace("$mainUrl/embed.html#","$mainUrl/$details")
        val server = app.get(link, allowRedirects = false).text
        val json = parseJson<Tomato>(server)
        if (json.status == 200) return listOf(
            ExtractorLink(
                name,
                name,
                json.file,
                "",
                Qualities.Unknown.value,
                isM3u8 = false
            )
        )
        return null
    }
}

package bhaee.stream.com.extractors

import bhaee.stream.com.app
import bhaee.stream.com.utils.ExtractorApi
import bhaee.stream.com.utils.ExtractorLink
import bhaee.stream.com.utils.Qualities
import bhaee.stream.com.utils.getAndUnpack

class Mp4Upload : ExtractorApi() {
    override var name = "Mp4Upload"
    override var mainUrl = "https://www.mp4upload.com"
    private val srcRegex = Regex("""player\.src\("(.*?)"""")
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        with(app.get(url)) {
            getAndUnpack(this.text).let { unpackedText ->
                srcRegex.find(unpackedText)?.groupValues?.get(1)?.let { link ->
                    return listOf(
                        ExtractorLink(
                            name,
                            name,
                            link,
                            url,
                            Qualities.Unknown.value,
                        )
                    )
                }
            }
        }
        return null
    }
}
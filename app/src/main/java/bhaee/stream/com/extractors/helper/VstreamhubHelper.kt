package bhaee.stream.com.extractors.helper

import bhaee.stream.com.app
import bhaee.stream.com.utils.ExtractorLink
import bhaee.stream.com.utils.Qualities
import bhaee.stream.com.utils.loadExtractor

class VstreamhubHelper {
    companion object {
        private val baseUrl: String = "https://vstreamhub.com"
        private val baseName: String = "Vstreamhub"

        suspend fun getUrls(url: String, callback: (ExtractorLink) -> Unit) {
            if (url.startsWith(baseUrl)) {
                // Fetch links
                val doc = app.get(url).document.select("script")
                doc?.forEach {
                    val innerText = it?.toString()
                    if (!innerText.isNullOrEmpty()) {
                        if (innerText.contains("file:")) {
                            val startString = "file: "
                            val aa = innerText.substring(innerText.indexOf(startString))
                            val linkUrl = aa.substring(startString.length + 1, aa.indexOf("\",")).trim()
                            //Log.i(baseName, "Result => (linkUrl) ${linkUrl}")
                            val exlink = ExtractorLink(
                                name = "$baseName m3u8",
                                source = baseName,
                                url = linkUrl,
                                quality = Qualities.Unknown.value,
                                referer = url,
                                isM3u8 = true
                            )
                            callback.invoke(exlink)
                        }
                        if (innerText.contains("playerInstance")) {
                            val aa = innerText.substring(innerText.indexOf("playerInstance.addButton"))
                            val startString = "window.open(["
                            val bb = aa.substring(aa.indexOf(startString))
                            val datavid = bb.substring(startString.length, bb.indexOf("]")).removeSurrounding("\"")
                            if (datavid.isNotBlank()) {
                                loadExtractor(datavid, url, callback)
                                //Log.i(baseName, "Result => (datavid) ${datavid}")
                            }
                        }
                    }
                }
            }
        }
    }
}
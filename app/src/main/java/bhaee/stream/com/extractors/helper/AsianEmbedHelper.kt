package bhaee.stream.com.extractors.helper

import android.util.Log
import bhaee.stream.com.apmap
import bhaee.stream.com.app
import bhaee.stream.com.utils.ExtractorLink
import bhaee.stream.com.utils.loadExtractor

class AsianEmbedHelper {
    companion object {
        suspend fun getUrls(url: String, callback: (ExtractorLink) -> Unit) {
            // Fetch links
            val doc = app.get(url).document
            val links = doc.select("div#list-server-more > ul > li.linkserver")
            if (!links.isNullOrEmpty()) {
                links.apmap {
                    val datavid = it.attr("data-video") ?: ""
                    //Log.i("AsianEmbed", "Result => (datavid) ${datavid}")
                    if (datavid.isNotBlank()) {
                        val res = loadExtractor(datavid, url, callback)
                        Log.i("AsianEmbed", "Result => ($res) (datavid) $datavid")
                    }
                }
            }
        }
    }
}
package bhaee.stream.com.metaproviders

import com.fasterxml.jackson.annotation.JsonProperty
import bhaee.stream.com.*
import bhaee.stream.com.APIHolder.apis
import bhaee.stream.com.APIHolder.getApiFromNameNull
import bhaee.stream.com.mvvm.logError
import bhaee.stream.com.utils.AppUtils.toJson
import bhaee.stream.com.utils.AppUtils.tryParseJson
import bhaee.stream.com.utils.ExtractorLink

class CrossTmdbProvider : TmdbProvider() {
    override var name = "MultiMovie"
    override val apiName = "MultiMovie"
    override val lang = "en"
    override val useMetaLoadResponse = true
    override val usesWebView = true
    override val supportedTypes = setOf(TvType.Movie)

    private fun filterName(name: String): String {
        return Regex("""[^a-zA-Z0-9-]""").replace(name, "")
    }

    private val validApis by lazy {
        apis.filter { it.lang == this.lang && it::class.java != this::class.java }
            //.distinctBy { it.uniqueId }
    }

    data class CrossMetaData(
        @JsonProperty("isSuccess") val isSuccess: Boolean,
        @JsonProperty("movies") val movies: List<Pair<String, String>>? = null,
    )

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        tryParseJson<CrossMetaData>(data)?.let { metaData ->
            if (!metaData.isSuccess) return false
            metaData.movies?.apmap { (apiName, data) ->
                getApiFromNameNull(apiName)?.let {
                    try {
                        it.loadLinks(data, isCasting, subtitleCallback, callback)
                    } catch (e: Exception) {
                        logError(e)
                    }
                }
            }

            return true
        }
        return false
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        return super.search(query)?.filterIsInstance<MovieSearchResponse>() // TODO REMOVE
    }

    override suspend fun load(url: String): LoadResponse? {
        val base = super.load(url)?.apply {
            this.recommendations = this.recommendations?.filterIsInstance<MovieSearchResponse>() // TODO REMOVE
            val matchName = filterName(this.name)
            when (this) {
                is MovieLoadResponse -> {
                    val data = validApis.apmap { api ->
                        try {
                            if (api.supportedTypes.contains(TvType.Movie)) { //|| api.supportedTypes.contains(TvType.AnimeMovie)
                                return@apmap api.search(this.name)?.first {
                                    if (filterName(it.name).equals(
                                            matchName,
                                            ignoreCase = true
                                        )
                                    ) {
                                        if (it is MovieSearchResponse)
                                            if (it.year != null && this.year != null && it.year != this.year) // if year exist then do a check
                                                return@first false

                                        return@first true
                                    }
                                    false
                                }?.let { search ->
                                    val response = api.load(search.url)
                                    if (response is MovieLoadResponse) {
                                        response
                                    } else {
                                        null
                                    }
                                }
                            }
                            null
                        } catch (e: Exception) {
                            logError(e)
                            null
                        }
                    }.filterNotNull()
                    this.dataUrl =
                        CrossMetaData(true, data.map { it.apiName to it.dataUrl }).toJson()
                }
                else -> {
                    throw ErrorLoadingException("Nothing besides movies are implemented for this provider")
                }
            }
        }

        return base
    }
}
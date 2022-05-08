package bhaee.stream.com.metaproviders

import bhaee.stream.com.*
import bhaee.stream.com.LoadResponse.Companion.addAniListId
import bhaee.stream.com.LoadResponse.Companion.addTrailer
import bhaee.stream.com.syncproviders.OAuth2API
import bhaee.stream.com.syncproviders.SyncAPI
import bhaee.stream.com.syncproviders.providers.AniListApi
import bhaee.stream.com.syncproviders.providers.MALApi
import bhaee.stream.com.utils.SyncUtil

// wont be implemented
class MultiAnimeProvider : MainAPI() {
    override var name = "MultiAnime"
    override val lang = "en"
    override val usesWebView = true
    override val supportedTypes = setOf(TvType.Anime)
    private val syncApi: SyncAPI = OAuth2API.aniListApi

    private val syncUtilType by lazy {
        when (syncApi) {
            is AniListApi -> "anilist"
            is MALApi -> "myanimelist"
            else -> throw ErrorLoadingException("Invalid Api")
        }
    }

    private val validApis by lazy {
        APIHolder.apis.filter {
            it.lang == this.lang && it::class.java != this::class.java && it.supportedTypes.contains(
                TvType.Anime
            )
        }
    }

    private fun filterName(name: String): String {
        return Regex("""[^a-zA-Z0-9-]""").replace(name, "")
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        return syncApi.search(query)?.map {
            AnimeSearchResponse(it.name, it.url, this.name, TvType.Anime, it.posterUrl)
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return syncApi.getResult(url)?.let { res ->
            val data = SyncUtil.getUrlsFromId(res.id, syncUtilType).apmap { url ->
                validApis.firstOrNull { api -> url.startsWith(api.mainUrl) }?.load(url)
            }.filterNotNull()

            val type =
                if (data.any { it.type == TvType.AnimeMovie }) TvType.AnimeMovie else TvType.Anime

            newAnimeLoadResponse(
                res.title ?: throw ErrorLoadingException("No Title found"),
                url,
                type
            ) {
                posterUrl = res.posterUrl
                plot = res.synopsis
                tags = res.genres
                rating = res.publicScore
                addTrailer(res.trailerUrl)
                addAniListId(res.id.toIntOrNull())
                recommendations = res.recommendations
            }
        }
    }
}
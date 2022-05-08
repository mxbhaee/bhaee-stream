package bhaee.stream.com.utils

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import bhaee.stream.com.app
import bhaee.stream.com.mapper
import bhaee.stream.com.mvvm.logError
import java.util.concurrent.TimeUnit

object SyncUtil {
    private val regexs = listOf(
        Regex("""(9anime)\.(?:to|center|id)/watch/(?:.*?)\.([^/?]*)"""),
        Regex("""(gogoanime|gogoanimes)\..*?/category/([^/?]*)"""),
        Regex("""(twist\.moe)/a/([^/?]*)"""),
    )

    private const val TAG = "SYNCUTIL"

    private const val GOGOANIME = "Gogoanime"
    private const val NINE_ANIME = "9anime"
    private const val TWIST_MOE = "Twistmoe"

    private val matchList =
        mapOf(
            "9anime" to NINE_ANIME,
            "gogoanime" to GOGOANIME,
            "gogoanimes" to GOGOANIME,
            "twist.moe" to TWIST_MOE
        )

    suspend fun getIdsFromUrl(url: String?): Pair<String?, String?>? {
        if (url == null) return null
        Log.i(TAG, "getIdsFromUrl $url")

        for (regex in regexs) {
            regex.find(url)?.let { match ->
                if (match.groupValues.size == 3) {
                    val site = match.groupValues[1]
                    val slug = match.groupValues[2]
                    matchList[site]?.let { realSite ->
                        getIdsFromSlug(slug, realSite)?.let {
                            return it
                        }
                    }
                }
            }
        }
        return null
    }

    /** first. Mal, second. Anilist,
     * valid sites are: Gogoanime, Twistmoe and 9anime*/
    private suspend fun getIdsFromSlug(
        slug: String,
        site: String = "Gogoanime"
    ): Pair<String?, String?>? {
        Log.i(TAG, "getIdsFromSlug $slug $site")
        try {
            //Gogoanime, Twistmoe and 9anime
            val url =
                "https://raw.githubusercontent.com/MALSync/MAL-Sync-Backup/master/data/pages/$site/$slug.json"
            val response = app.get(url, cacheTime = 1, cacheUnit = TimeUnit.DAYS).text
            val mapped = mapper.readValue<MalSyncPage?>(response)

            val overrideMal = mapped?.malId ?: mapped?.Mal?.id ?: mapped?.Anilist?.malId
            val overrideAnilist = mapped?.aniId ?: mapped?.Anilist?.id

            if (overrideMal != null) {
                return overrideMal.toString() to overrideAnilist?.toString()
            }
            return null
        } catch (e: Exception) {
            logError(e)
        }
        return null
    }

    suspend fun getUrlsFromId(id: String, type: String = "anilist") : List<String> {
        val url =
            "https://raw.githubusercontent.com/MALSync/MAL-Sync-Backup/master/data/$type/anime/$id.json"
        val response = app.get(url, cacheTime = 1, cacheUnit = TimeUnit.DAYS).parsed<SyncPage>()
        val pages = response.pages ?: return emptyList()
        return pages.gogoanime.values.union(pages.nineanime.values).union(pages.twistmoe.values).mapNotNull { it.url }
    }

    data class SyncPage(
        @JsonProperty("Pages") val pages: SyncPages?,
    )

    data class SyncPages(
        @JsonProperty("9anime") val nineanime: Map<String, ProviderPage> = emptyMap(),
        @JsonProperty("Gogoanime") val gogoanime: Map<String, ProviderPage> = emptyMap(),
        @JsonProperty("Twistmoe") val twistmoe: Map<String, ProviderPage> = emptyMap(),
    )

    data class ProviderPage(
        @JsonProperty("url") val url: String?,
    )

    data class MalSyncPage(
        @JsonProperty("identifier") val identifier: String?,
        @JsonProperty("type") val type: String?,
        @JsonProperty("page") val page: String?,
        @JsonProperty("title") val title: String?,
        @JsonProperty("url") val url: String?,
        @JsonProperty("image") val image: String?,
        @JsonProperty("hentai") val hentai: Boolean?,
        @JsonProperty("sticky") val sticky: Boolean?,
        @JsonProperty("active") val active: Boolean?,
        @JsonProperty("actor") val actor: String?,
        @JsonProperty("malId") val malId: Int?,
        @JsonProperty("aniId") val aniId: Int?,
        @JsonProperty("createdAt") val createdAt: String?,
        @JsonProperty("updatedAt") val updatedAt: String?,
        @JsonProperty("deletedAt") val deletedAt: String?,
        @JsonProperty("Mal") val Mal: Mal?,
        @JsonProperty("Anilist") val Anilist: Anilist?,
        @JsonProperty("malUrl") val malUrl: String?
    )

    data class Anilist(
//            @JsonProperty("altTitle") val altTitle: List<String>?,
//            @JsonProperty("externalLinks") val externalLinks: List<String>?,
        @JsonProperty("id") val id: Int?,
        @JsonProperty("malId") val malId: Int?,
        @JsonProperty("type") val type: String?,
        @JsonProperty("title") val title: String?,
        @JsonProperty("url") val url: String?,
        @JsonProperty("image") val image: String?,
        @JsonProperty("category") val category: String?,
        @JsonProperty("hentai") val hentai: Boolean?,
        @JsonProperty("createdAt") val createdAt: String?,
        @JsonProperty("updatedAt") val updatedAt: String?,
        @JsonProperty("deletedAt") val deletedAt: String?
    )

    data class Mal(
//            @JsonProperty("altTitle") val altTitle: List<String>?,
        @JsonProperty("id") val id: Int?,
        @JsonProperty("type") val type: String?,
        @JsonProperty("title") val title: String?,
        @JsonProperty("url") val url: String?,
        @JsonProperty("image") val image: String?,
        @JsonProperty("category") val category: String?,
        @JsonProperty("hentai") val hentai: Boolean?,
        @JsonProperty("createdAt") val createdAt: String?,
        @JsonProperty("updatedAt") val updatedAt: String?,
        @JsonProperty("deletedAt") val deletedAt: String?
    )
}
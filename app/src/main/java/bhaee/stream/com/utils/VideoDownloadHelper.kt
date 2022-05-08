package bhaee.stream.com.utils

import com.fasterxml.jackson.annotation.JsonProperty
import bhaee.stream.com.TvType
import bhaee.stream.com.ui.download.EasyDownloadButton

object VideoDownloadHelper {
    data class DownloadEpisodeCached(
        @JsonProperty("name") val name: String?,
        @JsonProperty("poster") val poster: String?,
        @JsonProperty("episode") val episode: Int,
        @JsonProperty("season") val season: Int?,
        @JsonProperty("id") override val id: Int,
        @JsonProperty("parentId") val parentId: Int,
        @JsonProperty("rating") val rating: Int?,
        @JsonProperty("description") val description: String?,
        @JsonProperty("cacheTime") val cacheTime: Long,
    ) : EasyDownloadButton.IMinimumData

    data class DownloadHeaderCached(
        @JsonProperty("apiName") val apiName: String,
        @JsonProperty("url") val url: String,
        @JsonProperty("type") val type: TvType,
        @JsonProperty("name") val name: String,
        @JsonProperty("poster") val poster: String?,
        @JsonProperty("id") val id: Int,
        @JsonProperty("cacheTime") val cacheTime: Long,
    )

    data class ResumeWatching(
        @JsonProperty("parentId") val parentId: Int,
        @JsonProperty("episodeId") val episodeId: Int?,
        @JsonProperty("episode") val episode: Int?,
        @JsonProperty("season") val season: Int?,
        @JsonProperty("updateTime") val updateTime: Long,
        @JsonProperty("isFromDownload") val isFromDownload: Boolean,
    )
}
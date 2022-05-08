package bhaee.stream.com.ui.search

import bhaee.stream.com.SearchQuality
import bhaee.stream.com.SearchResponse
import bhaee.stream.com.TvType
import bhaee.stream.com.syncproviders.OAuth2API

class SyncSearchViewModel {
    private val repos = OAuth2API.SyncApis

    data class SyncSearchResultSearchResponse(
        override val name: String,
        override val url: String,
        override val apiName: String,
        override var type: TvType?,
        override var posterUrl: String?,
        override var id: Int?,
        override var quality: SearchQuality? = null,
        override var posterHeaders: Map<String, String>? = null,
    ) : SearchResponse

}
package bhaee.stream.com.metaproviders

import bhaee.stream.com.ErrorLoadingException
import bhaee.stream.com.syncproviders.OAuth2API.Companion.SyncApis
import bhaee.stream.com.syncproviders.OAuth2API.Companion.aniListApi
import bhaee.stream.com.syncproviders.OAuth2API.Companion.malApi
import bhaee.stream.com.utils.SyncUtil

object SyncRedirector {
    val syncApis = SyncApis

    suspend fun redirect(url: String, preferredUrl: String): String {
        for (api in syncApis) {
            if (url.contains(api.mainUrl)) {
                val otherApi = when (api.name) {
                    aniListApi.name -> "anilist"
                    malApi.name -> "myanimelist"
                    else -> return url
                }

                return SyncUtil.getUrlsFromId(api.getIdFromUrl(url), otherApi).firstOrNull { realUrl ->
                    realUrl.contains(preferredUrl)
                } ?: run {
                    throw ErrorLoadingException("Page does not exist on $preferredUrl")
                }
            }
        }
        return url
    }
}
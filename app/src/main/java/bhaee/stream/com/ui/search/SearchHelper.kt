package bhaee.stream.com.ui.search

import android.app.Activity
import android.widget.Toast
import bhaee.stream.com.CommonActivity.showToast
import bhaee.stream.com.ui.download.DOWNLOAD_ACTION_PLAY_FILE
import bhaee.stream.com.ui.download.DownloadButtonSetup.handleDownloadClick
import bhaee.stream.com.ui.download.DownloadClickEvent
import bhaee.stream.com.ui.result.START_ACTION_LOAD_EP
import bhaee.stream.com.utils.AppUtils.loadSearchResult
import bhaee.stream.com.utils.DataStoreHelper
import bhaee.stream.com.utils.VideoDownloadHelper

object SearchHelper {
    fun handleSearchClickCallback(activity: Activity?, callback: SearchClickCallback) {
        val card = callback.card
        when (callback.action) {
            SEARCH_ACTION_LOAD -> {
                activity.loadSearchResult(card)
            }
            SEARCH_ACTION_PLAY_FILE -> {
                if (card is DataStoreHelper.ResumeWatchingResult) {
                    if (card.isFromDownload) {
                        handleDownloadClick(
                            activity, card.name, DownloadClickEvent(
                                DOWNLOAD_ACTION_PLAY_FILE,
                                VideoDownloadHelper.DownloadEpisodeCached(
                                    card.name,
                                    card.posterUrl,
                                    card.episode ?: 0,
                                    card.season,
                                    card.id!!,
                                    card.parentId ?: return,
                                    null,
                                    null,
                                    System.currentTimeMillis()
                                )
                            )
                        )
                    } else {
                        activity.loadSearchResult(card, START_ACTION_LOAD_EP, card.id)
                    }
                } else {
                    handleSearchClickCallback(
                        activity,
                        SearchClickCallback(SEARCH_ACTION_LOAD, callback.view, -1, callback.card)
                    )
                }
            }
            SEARCH_ACTION_SHOW_METADATA -> {
                showToast(activity, callback.card.name, Toast.LENGTH_SHORT)
            }
        }
    }
}
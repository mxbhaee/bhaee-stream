package bhaee.stream.com.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import bhaee.stream.com.APIHolder.apis
import bhaee.stream.com.APIHolder.filterProviderByPreferredMedia
import bhaee.stream.com.APIHolder.getApiFromNameNull
import bhaee.stream.com.AcraApplication.Companion.context
import bhaee.stream.com.AcraApplication.Companion.getKey
import bhaee.stream.com.AcraApplication.Companion.setKey
import bhaee.stream.com.HomePageResponse
import bhaee.stream.com.MainAPI
import bhaee.stream.com.SearchResponse
import bhaee.stream.com.mvvm.Resource
import bhaee.stream.com.mvvm.logError
import bhaee.stream.com.ui.APIRepository
import bhaee.stream.com.ui.APIRepository.Companion.noneApi
import bhaee.stream.com.ui.APIRepository.Companion.randomApi
import bhaee.stream.com.ui.WatchType
import bhaee.stream.com.utils.DOWNLOAD_HEADER_CACHE
import bhaee.stream.com.utils.DataStoreHelper
import bhaee.stream.com.utils.DataStoreHelper.getAllResumeStateIds
import bhaee.stream.com.utils.DataStoreHelper.getAllWatchStateIds
import bhaee.stream.com.utils.DataStoreHelper.getBookmarkedData
import bhaee.stream.com.utils.DataStoreHelper.getLastWatched
import bhaee.stream.com.utils.DataStoreHelper.getResultWatchState
import bhaee.stream.com.utils.DataStoreHelper.getViewPos
import bhaee.stream.com.utils.HOMEPAGE_API
import bhaee.stream.com.utils.VideoDownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class HomeViewModel : ViewModel() {
    private var repo: APIRepository? = null

    private val _apiName = MutableLiveData<String>()
    val apiName: LiveData<String> = _apiName

    private val _page = MutableLiveData<Resource<HomePageResponse?>>()
    val page: LiveData<Resource<HomePageResponse?>> = _page

    private val _randomItems = MutableLiveData<List<SearchResponse>?>(null)
    val randomItems: LiveData<List<SearchResponse>?> = _randomItems

    private fun autoloadRepo(): APIRepository {
        return APIRepository(apis.first { it.hasMainPage })
    }

    private val _availableWatchStatusTypes = MutableLiveData<Pair<EnumSet<WatchType>, EnumSet<WatchType>>>()
    val availableWatchStatusTypes: LiveData<Pair<EnumSet<WatchType>, EnumSet<WatchType>>> = _availableWatchStatusTypes
    private val _bookmarks = MutableLiveData<Pair<Boolean, List<SearchResponse>>>()
    val bookmarks: LiveData<Pair<Boolean, List<SearchResponse>>> = _bookmarks

    private val _resumeWatching = MutableLiveData<List<SearchResponse>>()
    val resumeWatching: LiveData<List<SearchResponse>> = _resumeWatching

    fun loadResumeWatching() = viewModelScope.launch {
        val resumeWatching = withContext(Dispatchers.IO) {
            getAllResumeStateIds()?.mapNotNull { id ->
                getLastWatched(id)
            }?.sortedBy { -it.updateTime }
        }

        // val resumeWatchingResult = ArrayList<DataStoreHelper.ResumeWatchingResult>()

        val resumeWatchingResult = withContext(Dispatchers.IO) {
            resumeWatching?.map { resume ->
                val data = getKey<VideoDownloadHelper.DownloadHeaderCached>(
                    DOWNLOAD_HEADER_CACHE,
                    resume.parentId.toString()
                ) ?: return@map null
                val watchPos = getViewPos(resume.episodeId)
                DataStoreHelper.ResumeWatchingResult(
                    data.name,
                    data.url,
                    data.apiName,
                    data.type,
                    data.poster,
                    watchPos,
                    resume.episodeId,
                    resume.parentId,
                    resume.episode,
                    resume.season,
                    resume.isFromDownload
                )
            }?.filterNotNull()
        }
        resumeWatchingResult?.let {
            _resumeWatching.postValue(it)
        }
    }

    fun loadStoredData(preferredWatchStatus: EnumSet<WatchType>?) = viewModelScope.launch {
        val watchStatusIds = withContext(Dispatchers.IO) {
            getAllWatchStateIds()?.map { id ->
                Pair(id, getResultWatchState(id))
            }
        }?.distinctBy { it.first } ?: return@launch

        val length = WatchType.values().size
        val currentWatchTypes = EnumSet.noneOf(WatchType::class.java)

        for (watch in watchStatusIds) {
            currentWatchTypes.add(watch.second)
            if (currentWatchTypes.size >= length) {
                break
            }
        }

        currentWatchTypes.remove(WatchType.NONE)

        if (currentWatchTypes.size <= 0) {
            _bookmarks.postValue(Pair(false, ArrayList()))
            return@launch
        }

        val watchPrefNotNull = preferredWatchStatus ?: EnumSet.of(currentWatchTypes.first())
        //if (currentWatchTypes.any { watchPrefNotNull.contains(it) }) watchPrefNotNull else listOf(currentWatchTypes.first())

        _availableWatchStatusTypes.postValue(
            Pair(
                watchPrefNotNull,
                currentWatchTypes,
            )
        )

        val list = withContext(Dispatchers.IO) {
            watchStatusIds.filter { watchPrefNotNull.contains(it.second) }
                .mapNotNull { getBookmarkedData(it.first) }
                .sortedBy { -it.latestUpdatedTime }
        }
        _bookmarks.postValue(Pair(true, list))
    }

    private var onGoingLoad: Job? = null
    private fun loadAndCancel(api: MainAPI?) {
        onGoingLoad?.cancel()
        onGoingLoad = load(api)
    }

    private fun load(api: MainAPI?) = viewModelScope.launch {
        repo = if (api != null) {
            APIRepository(api)
        } else {
            autoloadRepo()
        }

        _apiName.postValue(repo?.name)
        _randomItems.postValue(listOf())

        if (repo?.hasMainPage == true) {
            _page.postValue(Resource.Loading())

            val data = repo?.getMainPage()
            when (data) {
                is Resource.Success -> {
                    try {
                        val home = data.value
                        if (home?.items?.isNullOrEmpty() == false) {
                            val currentList =
                                home.items.shuffled().filter { !it.list.isNullOrEmpty() }.flatMap { it.list }
                                    .distinctBy { it.url }
                                    .toList()

                            if (!currentList.isNullOrEmpty()) {
                                val randomItems = currentList.shuffled()

                                _randomItems.postValue(randomItems)
                            }
                        }
                    } catch (e : Exception) {
                        _randomItems.postValue(emptyList())
                        logError(e)
                    }
                }
                else -> Unit
            }
            data?.let {
                _page.postValue(it)
            }
        } else {
            _page.postValue(Resource.Success(HomePageResponse(emptyList())))
        }
    }

    fun loadAndCancel(preferredApiName: String?) = viewModelScope.launch {
        val api = getApiFromNameNull(preferredApiName)
        if (preferredApiName == noneApi.name)
            loadAndCancel(noneApi)
        else if (preferredApiName == randomApi.name || api == null) {
            val validAPIs = context?.filterProviderByPreferredMedia()
            if(validAPIs.isNullOrEmpty()) {
                //loadAndCancel(noneApi)
                val apiRandom = validAPIs?.random()
                loadAndCancel(apiRandom)
                setKey(HOMEPAGE_API, apiRandom?.name)
            } else {
                val apiRandom = validAPIs.random()
                loadAndCancel(apiRandom)
                setKey(HOMEPAGE_API, apiRandom.name)
            }
        } else {
            loadAndCancel(api)
        }
    }
}
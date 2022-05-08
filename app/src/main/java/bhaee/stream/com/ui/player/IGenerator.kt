package bhaee.stream.com.ui.player

import bhaee.stream.com.utils.ExtractorLink
import bhaee.stream.com.utils.ExtractorUri

interface IGenerator {
    val hasCache: Boolean

    fun hasNext(): Boolean
    fun hasPrev(): Boolean
    fun next()
    fun prev()
    fun goto(index: Int)

    fun getCurrentId(): Int?                    // this is used to save data or read data about this id
    fun getCurrent(offset : Int = 0): Any?      // this is used to get metadata about the current playing, can return null

    /* not safe, must use try catch */
    suspend fun generateLinks(
        clearCache: Boolean,
        isCasting: Boolean,
        callback: (Pair<ExtractorLink?, ExtractorUri?>) -> Unit,
        subtitleCallback: (SubtitleData) -> Unit,
        offset : Int = 0,
    ): Boolean
}
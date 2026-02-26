package com.xlrr.roambendom.nav

import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.search.SearchParameterModel

sealed class Routes {
    sealed class Root : Routes() {
        companion object {
            val Default = Home
        }
        data object Home : Root()
        data class Detail(
            val searchItemData: SearchItemData
        ) : Root()
        open class SearchLike(
            val searchModel: SearchParameterModel,
            val canChangeSettings: Boolean
        ) : Root()
        class Search(
            searchModel: SearchParameterModel
        ) : SearchLike(searchModel, true)
        class History(searchParameterModel: SearchParameterModel)
            : SearchLike(searchParameterModel, false)
        data object Settings : Root()
    }
    data class Artwork(
        val artworkInfo: ArtworkInfo
    ) : Routes()
    data object TokenForm : Routes()
}
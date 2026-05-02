package com.xlrr.roambendom.nav

import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.model.detail.BaseDetailModel
import com.xlrr.roambendom.model.search.SearchParameterModel

sealed class Routes {
    sealed class Root(
        val headerTitle: String = ""
    ) : Routes() {
        companion object {
            val Default = Home
        }
        data object Home : Root()
        data class Detail(
            val detailModel: BaseDetailModel
        ) : Root()
        open class SearchLike(
            val searchModel: SearchParameterModel,
            val canChangeSettings: Boolean,
            val clearInput: (() -> Boolean)? = null,
            title: String = ""
        ) : Root(title)
        class Search(
            searchModel: SearchParameterModel
        ) : SearchLike(searchModel, true)
        class History(searchParameterModel: SearchParameterModel, clearFunc: () -> Boolean)
            : SearchLike(searchParameterModel, false, clearFunc)
        class FixedSearch(searchParameterModel: SearchParameterModel, title: String)
            : SearchLike(searchParameterModel, false, title = title)
        data object Settings : Root()
    }
    data class Artwork(
        val artworkInfo: ArtworkInfo
    ) : Routes()
    data object TokenForm : Routes()
}

fun Routes.Root.SearchLike.shouldShowTrailingIcon() : Boolean {
    return canChangeSettings || clearInput != null
}
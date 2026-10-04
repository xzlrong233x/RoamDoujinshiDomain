package com.xlrr.roambendom.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.manager.FavoriteDataManager
import com.xlrr.roambendom.model.FavoriteScreenModel
import com.xlrr.roambendom.model.detail.BaseDetailModel
import com.xlrr.roambendom.model.search.SearchParameterModel
import com.xlrr.roambendom.utils.StringOrResource
import com.xlrr.roambendom.utils.orResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.favorite_with_info

sealed class Routes {
    sealed class Root(
        val headerTitle: () -> StringOrResource = { StringOrResource.EMPTY },
        val endCompose: @Composable () -> Unit = {},
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
            title: () -> StringOrResource = { StringOrResource.EMPTY },
        ) : Root(title)
        class Search(
            searchModel: SearchParameterModel
        ) : SearchLike(searchModel, true)
        class History(searchParameterModel: SearchParameterModel, clearFunc: () -> Boolean)
            : SearchLike(searchParameterModel, false, clearFunc)
        class Favorite(
            val path: String = "/"
        ) : FixedSearch(FavoriteScreenModel(path), {
            Res.string.favorite_with_info.orResource(FavoriteDataManager.content.folderInfo[path]?.name.toString()) //favorite_with_info
        })
        open class FixedSearch(searchParameterModel: SearchParameterModel, title: () -> StringOrResource)
            : SearchLike(searchParameterModel, false, title = title)
        data object Download : Root()
        data object Settings : Root()
    }
    data class Artwork(
        val artworkInfo: ArtworkInfo,
        val pageChange: (Int) -> Unit = {},
    ) : Routes()
    sealed class Auth : Routes() {
        data class Choose(
            var callback: ((String, MutableState<Boolean>) -> Unit)? = null,
            val called: MutableState<Boolean> = mutableStateOf(false)
        ) : Auth()
        data object Wait : Auth()
    }
}

fun Routes.Root.SearchLike.shouldShowTrailingIcon() : Boolean {
    return canChangeSettings || clearInput != null
}
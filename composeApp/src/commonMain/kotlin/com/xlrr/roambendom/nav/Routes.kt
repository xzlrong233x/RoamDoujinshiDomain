package com.xlrr.roambendom.nav

import com.xlrr.roambendom.data.ArtworkInfo
import com.xlrr.roambendom.data.SearchItemData

sealed class Routes {
    sealed class Root : Routes() {
        companion object {
            val Default = Home
        }
        data object Home : Root()
        data class Detail(
            val searchItemData: SearchItemData
        ) : Root()
        data class Search(
            val key: String
        ) : Root()
        data object Settings : Root()
    }
    data class Artwork(
        val artworkInfo: ArtworkInfo
    ) : Routes()
}
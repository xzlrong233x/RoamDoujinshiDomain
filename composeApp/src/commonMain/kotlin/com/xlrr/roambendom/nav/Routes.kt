package com.xlrr.roambendom.nav

sealed class Routes {
    sealed class Root : Routes() {
        companion object {
            val Default = Home
        }
        data object Home : Root()
        data class Search(
            val key: String
        ) : Root()
        data object Settings : Root()
    }
}
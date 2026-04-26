package com.xlrr.roambendom.utils

import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.data.SearchResult
import com.xlrr.roambendom.data.search.SearchParameterModel
import com.xlrr.roambendom.nav.Navigator
import com.xlrr.roambendom.nav.Routes
import com.xlrr.roambendom.network.PIXIVApiHelper

fun Navigator.pushAuthorSearch(userId: String, userName: String) {
    push(Routes.Root.FixedSearch(
        SearchParameterModel(userId).config {
            searchFunction = {k,p,e ->
                if (p == 1) {
                    val uaw = PIXIVApiHelper.userAllWorks(k)
                    val al = ArrayList<Int>()
                    al.addAll(uaw.illust)
                    al.addAll(uaw.manga)
                    al.sortDescending()
                    e["artworks"] = al.toList()
                }
                var l: List<SearchItemData> = listOf()
                var tot = 0
                val aw = e["artworks"]
                if (aw is List<*>) {
                    tot = aw.size
                    val work = aw.map { it as Int }
                    MthUtil.calWindow(p, 48, tot).let {
                        l = PIXIVApiHelper.userWorkInfo(work.subList(it.first, it.second), k)
                    }
                }
                SearchResult(
                    tot,
                    l,
                    k,
                    p
                )
            }
        }, "${userName}的作品"
    ))
}
package com.xlrr.roambendom.model.detail

import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.network.NHWebHelper

class NHDetailModel(searchItemData: SearchItemData) : BaseDetailModel(searchItemData) {
    override suspend fun request() {
        content = NHWebHelper.artwork(searchItemData.id)
    }
}
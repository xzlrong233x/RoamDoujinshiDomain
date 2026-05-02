package com.xlrr.roambendom.model.detail

import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import com.xlrr.roambendom.network.PIXIVApiHelper

class PIXIVDetailModel(searchItemData: SearchItemData) : BaseDetailModel(searchItemData) {
    override suspend fun request() {
        content = PIXIVApiHelper.artwork(searchItemData.id)
    }
}
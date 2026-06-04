package com.xlrr.roambendom.data.pixiv.search

import com.xlrr.roambendom.data.CLanguage
import com.xlrr.roambendom.data.CRestriction
import com.xlrr.roambendom.data.CSources
import com.xlrr.roambendom.data.SearchItemData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.time.Instant

@Serializable
data class ArtworkDetailData(
    val id: Int,
    val title: String,
    val type: String,
    @SerialName("image_urls")
    val imageUrls: ImageUrls,
    val caption: String,
    val restrict: Int,
    val user: User,
    val tags: List<Tag>,
    val tools: List<String>,
    @SerialName("create_date")
    val createDate: String,
    @SerialName("page_count")
    val pageCount: Int,
    val width: Int,
    val height: Int,
    @SerialName("sanity_level")
    val sanityLevel: Int,
    @SerialName("x_restrict")
    val xRestrict: Int,
    val series: JsonElement? = null,
    @SerialName("meta_single_page")
    val metaSinglePage: MetaSinglePage,
    @SerialName("meta_pages")
    val metaPages: List<MetaPage>,
    @SerialName("total_view")
    val totalView: Int,
    @SerialName("total_bookmarks")
    val totalBookmarks: Int,
    @SerialName("is_bookmarked")
    val isBookmarked: Boolean,
    val visible: Boolean,
    @SerialName("is_muted")
    val isMuted: Boolean,
    @SerialName("seasonal_effect_animation_urls")
    val seasonalEffectAnimationUrls: JsonElement? = null,
    @SerialName("event_banners")
    val eventBanners: JsonElement? = null,
    @SerialName("illust_ai_type")
    val illustAIType: Int,
    @SerialName("illust_book_style")
    val illustBookStyle: Int,
    val request: JsonElement? = null,
    @SerialName("restriction_attributes")
    val restrictionAttributes: List<String> = listOf()
) {
    companion object {
        fun ArtworkDetailData.toSearchItem() = SearchItemData(
            id.toString(),
            CSources.PIXIV,
            title,
            pageCount,
            CLanguage.Unknown,
            imageUrls.medium,
            width,
            height,
            CRestriction.entries[xRestrict],
            illustAIType > 1,
            "${user.name}(${user.id})",
            Instant.parse(createDate).toEpochMilliseconds(),
            type == "ugoira"
        )
    }
}
@Serializable
data class ImageUrls (
    @SerialName("square_medium")
    val squareMedium: String = "",
    val medium: String = "",
    val large: String = "",
    val original: String? = null
)
@Serializable
data class MetaPage (
    @SerialName("image_urls")
    val imageUrls: ImageUrls
)
@Serializable
data class MetaSinglePage(
    @SerialName("original_image_url")
    val originalImageURL: String = ""
)
@Serializable
data class Tag (
    val name: String,
    @SerialName("translated_name")
    val translatedName: String? = null
)
@Serializable
data class User (
    val id: Int,
    val name: String,
    val account: String,
    @SerialName("profile_image_urls")
    val profileImageUrls: ProfileImageUrls,
    @SerialName("is_followed")
    val isFollowed: Boolean,
    @SerialName("is_accept_request")
    val isAcceptRequest: Boolean
)
@Serializable
data class ProfileImageUrls (
    val medium: String
)
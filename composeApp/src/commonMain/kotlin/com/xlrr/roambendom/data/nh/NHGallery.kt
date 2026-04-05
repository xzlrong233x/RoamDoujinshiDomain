package com.xlrr.roambendom.data.nh

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NHGallery(
    val id: Int,
    @SerialName("media_id")
    val mediaId: String,
    val title: NHGalleryTitle,
    val cover: NHGalleryCover,
    val thumbnail: NHGalleryThumbnail,
    val scanlator: String,
    @SerialName("upload_date")
    val uploadDate: Long,
    val tags: List<NHGalleryTagItem>,
    @SerialName("num_pages")
    val numPages: Int,
    @SerialName("num_favorites")
    val numFavorites: Int,
    val pages: List<NHGalleryPageItem>,
)

@Serializable
data class NHGalleryTitle(
    val english: String,
    val japanese: String?,
    val pretty: String,
)

@Serializable
data class NHGalleryCover(
    val path: String,
    val width: Int,
    val height: Int,
)

@Serializable
data class NHGalleryThumbnail(
    val path: String,
    val width: Int,
    val height: Int,
)

@Serializable
data class NHGalleryTagItem(
    val id: Int,
    val type: NHGalleryTagType,
    val name: String,
    val slug: String,
    val url: String,
    val count: Int,
)

@Serializable
enum class NHGalleryTagType {
    language,
    tag,
    artist,
    group,
    parody,
    character,
    category;

    override fun toString(): String {
        return super.toString().lowercase()
    }
}

@Serializable
data class NHGalleryPageItem(
    val number: Int,
    val path: String,
    val width: Int,
    val height: Int,
    val thumbnail: String,
    @SerialName("thumbnail_width")
    val thumbnailWidth: Int,
    @SerialName("thumbnail_height")
    val thumbnailHeight: Int,
)


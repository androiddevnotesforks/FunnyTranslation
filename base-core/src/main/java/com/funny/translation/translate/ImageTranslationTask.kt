package com.funny.translation.translate

import com.google.gson.annotations.SerializedName
import okhttp3.MediaType

data class ImageTranslationPart(
    val source: String,
    var target: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

class ImageTranslationResult(
    @SerializedName("erased_img")
    val erasedImgBase64: String? = null,
    val source: String = "",
    val target: String = "",
    val content: List<ImageTranslationPart> = arrayListOf()
)

abstract class ImageTranslationTask(
    val sourceImg: ByteArray = byteArrayOf(),
) : CoreTranslationTask() {
    val result = ImageTranslationResult()

    abstract fun translate()
    abstract val isOffline: Boolean
}
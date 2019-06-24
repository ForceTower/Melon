package com.forcetower.uefs.core.model.api

internal data class UploadResponse(
    val data: ImgurUpload,
    val success: Boolean,
    val status: Int
)

data class ImgurUpload(
    val link: String,
    val deletehash: String
)
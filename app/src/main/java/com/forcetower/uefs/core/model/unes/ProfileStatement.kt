package com.forcetower.uefs.core.model.unes

data class ProfileStatement(
    val id: Long,
    val senderId: Long,
    val senderName: String,
    val senderPicture: String?,
    val text: String,
    val likes: Int,
    val approved: Boolean,
    val createdAt: Long
)

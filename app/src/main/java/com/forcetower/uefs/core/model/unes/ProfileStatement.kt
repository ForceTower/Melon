package com.forcetower.uefs.core.model.unes

data class ProfileStatement(
    val id: Long,
    val senderId: Long,
    val senderName: String,
    val picture: String?,
    val text: String,
    val likes: Int,
    val approved: Boolean
)

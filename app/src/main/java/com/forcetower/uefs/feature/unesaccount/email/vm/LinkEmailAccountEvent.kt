package com.forcetower.uefs.feature.unesaccount.email.vm

sealed interface LinkEmailAccountEvent {
    data object InvalidEmail : LinkEmailAccountEvent
    data class EmailSent(val token: String, val email: String) : LinkEmailAccountEvent
    data object InvalidInfo : LinkEmailAccountEvent
    data object SendError : LinkEmailAccountEvent
}
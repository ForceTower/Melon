package com.forcetower.uefs.feature.unesaccount.confirm.vm

sealed interface ConfirmEmailAccountEvent {
    data object Completed : ConfirmEmailAccountEvent
    data object InvalidCode : ConfirmEmailAccountEvent
    data object EmailTaken : ConfirmEmailAccountEvent
    data object ConnectionFailed : ConfirmEmailAccountEvent
    data object TooManyTries : ConfirmEmailAccountEvent
}

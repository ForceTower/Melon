package com.forcetower.uefs.feature.unesaccount.login.vm

sealed interface LoginAccountEvent {
    data object LoginSuccess : LoginAccountEvent
    data object LoginFailed : LoginAccountEvent
}
package com.forcetower.uefs.feature.unesaccount.login.vm

sealed interface LoginAccountEvent {
    data object SuccessHasEmail : LoginAccountEvent
    data object SuccessLinkEmail : LoginAccountEvent
    data object LoginFailed : LoginAccountEvent
    data class StartPasskeyAssertion(val flowId: String, val json: String) : LoginAccountEvent
}

package com.forcetower.uefs.feature.unesaccount.overview.vm

sealed interface AccountOverviewEvent {
    data object PasskeyRegisterConnectionFailed : AccountOverviewEvent
    data object PasskeyRegisterCompleted : AccountOverviewEvent
    data object ImageUpdateFailed : AccountOverviewEvent
    data class PasskeyRegister(val flowId: String, val json: String): AccountOverviewEvent
}
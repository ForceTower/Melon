package com.forcetower.uefs.core.model.ui.edge

sealed interface EmailLinkComplete {
    data object Linked : EmailLinkComplete
    data object InvalidCode : EmailLinkComplete
    data object EmailTaken : EmailLinkComplete
    data object TooManyTries : EmailLinkComplete
    data object ConnectionError : EmailLinkComplete
}
package com.forcetower.uefs.core.model.ui.edge

sealed interface EmailLinkStart {
    data class CodeSent(val securityCode: String): EmailLinkStart
    data object InvalidInfo : EmailLinkStart
    data object ConnectionError : EmailLinkStart
}
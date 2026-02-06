package com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel

internal sealed interface CatalogEvent {
    data object NavigateBack : CatalogEvent
}
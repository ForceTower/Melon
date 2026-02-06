package com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel

import com.forcetower.uefs.ui.arch.ArchViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
internal class CatalogViewModel : ArchViewModel<CatalogState, CatalogEvent, CatalogIntent>(CatalogState()) {
    override suspend fun handleIntent(intent: CatalogIntent) {

    }
}
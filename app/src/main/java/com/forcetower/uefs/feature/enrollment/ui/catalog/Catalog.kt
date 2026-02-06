package com.forcetower.uefs.feature.enrollment.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CatalogIntent
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CatalogState
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CatalogViewModel
import com.forcetower.uefs.ui.theme.MelonTheme

@Composable
internal fun Catalog(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CatalogContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun CatalogContent(
    state: CatalogState,
    onIntent: (CatalogIntent) -> Unit,
    modifier: Modifier = Modifier
) {

}

@Preview
@Composable
private fun CatalogContentPreview() {
    MelonTheme(dynamicColor = false) {
        CatalogContent(
            state = CatalogState(),
            onIntent = {},
        )
    }
}
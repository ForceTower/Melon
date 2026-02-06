package com.forcetower.uefs.feature.enrollment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.forcetower.uefs.feature.enrollment.ui.catalog.Catalog
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CatalogViewModel
import com.forcetower.uefs.ui.theme.MelonTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EnrollmentActivity : ComponentActivity() {
    private val viewModel by viewModels<CatalogViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelonTheme {
                Catalog(viewModel = viewModel)
            }
        }
    }
}

package dev.forcetower.unes.ui.feature.licenses

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface LicensesIntent : UiIntent
internal sealed interface LicensesEffect : UiEffect

internal data class LicensesUiState(
    // `null` until the asset has been loaded — the screen renders a spinner
    // for that window. Empty list means "loaded but nothing there", which is
    // the fresh-checkout case before the Licensee task has run; the screen
    // surfaces an explicit message for that.
    val packages: List<LicensePackage>? = null,
    // Byte size of the bundled `artifacts.json` — drives the "Licensee · JSON ·
    // N KB" meta on the export row. `null` while loading / when the asset is
    // absent (the export row hides itself in that case).
    val manifestSizeBytes: Int? = null,
) : UiState

@HiltViewModel
internal class LicensesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : MviViewModel<LicensesUiState, LicensesIntent, LicensesEffect>(LicensesUiState()) {

    init {
        viewModelScope.launch {
            val (loaded, size) = withContext(Dispatchers.IO) {
                LicensesAssetLoader.load(context) to LicensesAssetLoader.rawManifest(context)?.size
            }
            setState { copy(packages = loaded, manifestSizeBytes = size) }
        }
    }

    override fun onIntent(intent: LicensesIntent) = Unit
}

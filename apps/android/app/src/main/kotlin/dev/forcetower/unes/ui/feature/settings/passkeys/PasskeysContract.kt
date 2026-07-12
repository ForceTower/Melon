package dev.forcetower.unes.ui.feature.settings.passkeys

import android.app.Activity
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState

// One registered passkey, shaped for the row + detail sheet. `name` stays
// nullable so the screen can fall back to a localized label; `createdAtLabel`
// is pre-formatted with the device locale by the ViewModel.
internal data class PasskeyItem(
    val id: String,
    val name: String?,
    val isSynced: Boolean,
    val createdAtLabel: String,
)

// Where a new passkey is saved. Pins the system picker's
// `authenticatorAttachment` so "Este dispositivo" enrolls a platform passkey
// and "Chave de segurança" targets a roaming security key.
internal enum class PasskeyTarget(val attachment: String) {
    ThisDevice("platform"),
    SecurityKey("cross-platform"),
}

internal enum class PasskeySheet { None, Add, Detail }

internal enum class AddStep { Choose, Auth, Success }

// Transient confirmation banners; each maps to its own copy + glyph + tint in
// the screen. `Error` is the catch-all for a failed create/rename/delete.
internal enum class PasskeyToast { Created, Renamed, Deleted, Error }

internal data class PasskeysUiState(
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val items: List<PasskeyItem> = emptyList(),
    val sheet: PasskeySheet = PasskeySheet.None,
    val addStep: AddStep = AddStep.Choose,
    val target: PasskeyTarget = PasskeyTarget.ThisDevice,
    val detailId: String? = null,
    val editing: Boolean = false,
    val editName: String = "",
    val savingName: Boolean = false,
    val confirmDeleteId: String? = null,
    val deleting: Boolean = false,
    val toast: PasskeyToast? = null,
    val newlyAddedId: String? = null,
) : UiState {
    val isEmpty: Boolean get() = !loading && !loadError && items.isEmpty()
    val detail: PasskeyItem? get() = items.firstOrNull { it.id == detailId }
    val confirmTarget: PasskeyItem? get() = items.firstOrNull { it.id == confirmDeleteId }
}

internal sealed interface PasskeysIntent : UiIntent {
    data object Load : PasskeysIntent
    data object Refresh : PasskeysIntent
    data object OpenAdd : PasskeysIntent
    data class SelectTarget(val target: PasskeyTarget) : PasskeysIntent
    data class ContinueAdd(val activity: Activity) : PasskeysIntent
    data object CloseSheet : PasskeysIntent
    data class OpenDetail(val id: String) : PasskeysIntent
    data object StartEdit : PasskeysIntent
    data class EditNameChanged(val value: String) : PasskeysIntent
    data object SaveName : PasskeysIntent
    data object RequestDelete : PasskeysIntent
    data object CancelDelete : PasskeysIntent
    data object ConfirmDelete : PasskeysIntent
    data object DismissToast : PasskeysIntent
}

internal sealed interface PasskeysEffect : UiEffect

package dev.forcetower.unes.ui.feature.settings.passkeys

import android.app.Activity
import android.os.Build
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.analytics.ContentTypes
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.model.PasskeyCredential
import dev.forcetower.melon.feature.auth.domain.model.PasskeyError
import dev.forcetower.melon.feature.auth.domain.usecase.DeletePasskeyUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.GetPasskeyRegistrationOptionsUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.ListPasskeysUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.RegisterPasskeyUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.RenamePasskeyUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.ui.feature.onboarding.login.PasskeyClient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Drives the "Chaves de acesso" screen (dc `PasskeysScreen`). Registration is
// a two-legged WebAuthn dance — fetch server options, hand them to the system
// CredentialManager sheet, then verify the attestation — bracketed by the
// Choose → Auth → Success sheet steps. List/rename/delete are single calls that
// optimistically re-list afterward. Toasts auto-dismiss on a timer.
@HiltViewModel
internal class PasskeysViewModel @Inject constructor(
    private val listPasskeys: ListPasskeysUseCase,
    private val getRegistrationOptions: GetPasskeyRegistrationOptionsUseCase,
    private val registerPasskey: RegisterPasskeyUseCase,
    private val renamePasskey: RenamePasskeyUseCase,
    private val deletePasskey: DeletePasskeyUseCase,
    private val passkeyClient: PasskeyClient,
    private val analytics: Analytics,
) : MviViewModel<PasskeysUiState, PasskeysIntent, PasskeysEffect>(PasskeysUiState()) {

    private var toastJob: Job? = null

    init {
        load(initial = true)
    }

    override fun onIntent(intent: PasskeysIntent) {
        when (intent) {
            PasskeysIntent.Load -> load(initial = true)
            PasskeysIntent.Refresh -> load(initial = false)
            PasskeysIntent.OpenAdd -> setState {
                copy(sheet = PasskeySheet.Add, addStep = AddStep.Choose, target = PasskeyTarget.ThisDevice)
            }
            is PasskeysIntent.SelectTarget -> setState { copy(target = intent.target) }
            is PasskeysIntent.ContinueAdd -> continueAdd(intent.activity)
            PasskeysIntent.CloseSheet -> setState {
                copy(sheet = PasskeySheet.None, addStep = AddStep.Choose, editing = false, confirmDeleteId = null)
            }
            is PasskeysIntent.OpenDetail -> setState {
                val item = items.firstOrNull { it.id == intent.id }
                copy(sheet = PasskeySheet.Detail, detailId = intent.id, editing = false, editName = item?.name.orEmpty())
            }
            PasskeysIntent.StartEdit -> setState { copy(editing = true, editName = detail?.name.orEmpty()) }
            is PasskeysIntent.EditNameChanged -> setState { copy(editName = intent.value) }
            PasskeysIntent.SaveName -> saveName()
            PasskeysIntent.RequestDelete -> setState { copy(confirmDeleteId = detailId) }
            PasskeysIntent.CancelDelete -> setState { copy(confirmDeleteId = null) }
            PasskeysIntent.ConfirmDelete -> confirmDelete()
            PasskeysIntent.DismissToast -> setState { copy(toast = null) }
        }
    }

    private fun load(initial: Boolean) {
        if (initial) setState { copy(loading = true, loadError = false) }
        viewModelScope.launch {
            when (val outcome = listPasskeys()) {
                is Outcome.Ok -> setState {
                    copy(loading = false, loadError = false, items = outcome.value.map { it.toItem() })
                }
                is Outcome.Err -> setState {
                    // A background refresh keeps the stale list; only the first
                    // load surfaces the error state.
                    if (initial) copy(loading = false, loadError = true) else this
                }
            }
        }
    }

    private fun continueAdd(activity: Activity) {
        analytics.selectContent(ContentTypes.PASSKEY, properties = mapOf("action" to "create"))
        setState { copy(addStep = AddStep.Auth) }
        viewModelScope.launch {
            val options = when (val outcome = getRegistrationOptions()) {
                is Outcome.Ok -> outcome.value
                is Outcome.Err -> return@launch abortAdd(toastError = true)
            }

            val attestation = try {
                passkeyClient.register(
                    requestJson = options.requestJson,
                    activity = activity,
                    authenticatorAttachment = currentState.target.attachment,
                )
            } catch (cancelled: PasskeyClient.PasskeyException.Cancelled) {
                // User dismissed the system sheet — quietly return to the picker.
                return@launch abortAdd(toastError = false)
            } catch (e: PasskeyClient.PasskeyException) {
                return@launch abortAdd(toastError = true)
            }

            when (registerPasskey(attestation, deviceName = defaultDeviceName())) {
                is Outcome.Ok -> finishAdd()
                is Outcome.Err -> abortAdd(toastError = true)
            }
        }
    }

    private fun abortAdd(toastError: Boolean) {
        if (toastError) {
            // Close the sheet so the error toast isn't hidden behind the modal;
            // a silent cancel just steps back to the picker.
            setState { copy(sheet = PasskeySheet.None, addStep = AddStep.Choose) }
            showToast(PasskeyToast.Error)
        } else {
            setState { copy(addStep = AddStep.Choose) }
        }
    }

    private suspend fun finishAdd() {
        setState { copy(addStep = AddStep.Success) }
        delay(SUCCESS_DWELL_MS)
        val refreshed = (listPasskeys() as? Outcome.Ok)?.value?.map { it.toItem() } ?: currentState.items
        setState {
            copy(
                sheet = PasskeySheet.None,
                addStep = AddStep.Choose,
                items = refreshed,
                newlyAddedId = refreshed.firstOrNull()?.id,
            )
        }
        showToast(PasskeyToast.Created)
        viewModelScope.launch {
            delay(FLASH_MS)
            setState { copy(newlyAddedId = null) }
        }
    }

    private fun saveName() {
        val id = currentState.detailId ?: return
        val name = currentState.editName.trim()
        if (name.isEmpty()) {
            setState { copy(editing = false) }
            return
        }
        analytics.selectContent(ContentTypes.PASSKEY, id, mapOf("action" to "rename"))
        setState { copy(savingName = true) }
        viewModelScope.launch {
            when (val outcome = renamePasskey(id, name)) {
                is Outcome.Ok -> {
                    refreshItems()
                    setState { copy(editing = false, savingName = false) }
                    showToast(PasskeyToast.Renamed)
                }
                is Outcome.Err -> {
                    setState { copy(savingName = false) }
                    if (outcome.error is PasskeyError.NotFound) {
                        refreshItems()
                        setState { copy(sheet = PasskeySheet.None, editing = false) }
                    }
                    showToast(PasskeyToast.Error)
                }
            }
        }
    }

    private fun confirmDelete() {
        val id = currentState.confirmDeleteId ?: return
        analytics.selectContent(ContentTypes.PASSKEY, id, mapOf("action" to "revoke"))
        setState { copy(deleting = true) }
        viewModelScope.launch {
            when (deletePasskey(id)) {
                is Outcome.Ok -> {
                    refreshItems()
                    setState { copy(deleting = false, confirmDeleteId = null, sheet = PasskeySheet.None) }
                    showToast(PasskeyToast.Deleted)
                }
                is Outcome.Err -> {
                    // NotFound means it's already gone; either way drop it and close.
                    refreshItems()
                    setState { copy(deleting = false, confirmDeleteId = null, sheet = PasskeySheet.None) }
                    showToast(PasskeyToast.Error)
                }
            }
        }
    }

    private suspend fun refreshItems() {
        val refreshed = (listPasskeys() as? Outcome.Ok)?.value?.map { it.toItem() } ?: return
        setState { copy(items = refreshed) }
    }

    private fun showToast(toast: PasskeyToast) {
        toastJob?.cancel()
        setState { copy(toast = toast) }
        toastJob = viewModelScope.launch {
            delay(TOAST_MS)
            setState { copy(toast = null) }
        }
    }

    private fun PasskeyCredential.toItem(): PasskeyItem = PasskeyItem(
        id = id,
        name = deviceName?.takeIf { it.isNotBlank() },
        isSynced = isSynced,
        createdAtLabel = formatCreated(createdAt),
    )

    // Locale-aware medium date; the wire value is an ISO-8601 instant.
    private fun formatCreated(iso: String): String = try {
        CreatedFormatter.format(Instant.parse(iso))
    } catch (e: Exception) {
        ""
    }

    private fun defaultDeviceName(): String =
        Build.MODEL?.takeIf { it.isNotBlank() } ?: Build.MANUFACTURER.orEmpty()

    private companion object {
        const val SUCCESS_DWELL_MS = 1100L
        const val TOAST_MS = 2200L
        const val FLASH_MS = 1800L
        val CreatedFormatter: DateTimeFormatter
            get() = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
                .withZone(ZoneId.systemDefault())
    }
}

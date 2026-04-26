package dev.forcetower.unes.ui.feature.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.melon.feature.messages.domain.usecase.MarkMessageAsReadUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessageDetailUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessagesInboxUseCase
import dev.forcetower.unes.mvi.MviViewModel
import dev.forcetower.unes.mvi.UiEffect
import dev.forcetower.unes.mvi.UiIntent
import dev.forcetower.unes.mvi.UiState
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedDetail as KmpMessageFeedDetail
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedItem as KmpMessageFeedItem

// "Mensagens" tab. Mirrors `MessagesListViewModel` + `MessageDetailViewModel`
// on iOS, collapsed into a single MVI VM: the inbox flow lives here, and the
// per-message detail flow is started/cancelled as a child job whenever a row
// is opened. Mark-as-read is idempotent on the KMP side, so list-tap and
// detail-appear can both call it without coordinating.
internal sealed interface MessagesIntent : UiIntent {
    data class OpenMessage(val id: String, val seed: KmpMessageFeedItem) : MessagesIntent
    data object CloseMessage : MessagesIntent
    data class SetFilter(val filter: MessageFilter) : MessagesIntent
    data class MarkRead(val id: String) : MessagesIntent
}

internal sealed interface MessagesEffect : UiEffect

internal data class MessagesUiState(
    val rawItems: List<KmpMessageFeedItem> = emptyList(),
    val openMessageId: String? = null,
    val openSeed: KmpMessageFeedItem? = null,
    val openDetail: KmpMessageFeedDetail? = null,
    val filter: MessageFilter = MessageFilter.All,
    val isLoading: Boolean = true,
) : UiState

@HiltViewModel
internal class MessagesViewModel @Inject constructor(
    private val observeInbox: ObserveMessagesInboxUseCase,
    private val observeDetail: ObserveMessageDetailUseCase,
    private val markReadUseCase: MarkMessageAsReadUseCase,
    private val savedStateHandle: SavedStateHandle,
) : MviViewModel<MessagesUiState, MessagesIntent, MessagesEffect>(MessagesUiState()) {

    private var detailJob: Job? = null

    init {
        viewModelScope.launch {
            observeInbox().collect { items ->
                setState { copy(rawItems = items, isLoading = false) }
            }
        }
        savedStateHandle.get<String>(KEY_OPEN_ID)?.let { restored ->
            // Re-attach a detail subscription after process death — we don't
            // have the original seed, so the screen falls back to whatever
            // the detail flow emits first. Initial detail is null until the
            // flow lands; the row card itself is gone but the detail screen
            // still shows the back button + its own ambient glow.
            setState { copy(openMessageId = restored) }
            startDetail(restored)
        }
    }

    override fun onIntent(intent: MessagesIntent) {
        when (intent) {
            is MessagesIntent.OpenMessage -> open(intent.id, intent.seed)
            MessagesIntent.CloseMessage -> close()
            is MessagesIntent.SetFilter -> setState { copy(filter = intent.filter) }
            is MessagesIntent.MarkRead -> markRead(intent.id)
        }
    }

    private fun open(id: String, seed: KmpMessageFeedItem) {
        savedStateHandle[KEY_OPEN_ID] = id
        setState { copy(openMessageId = id, openSeed = seed, openDetail = null) }
        startDetail(id)
        markRead(id)
    }

    private fun close() {
        detailJob?.cancel()
        detailJob = null
        savedStateHandle.remove<String>(KEY_OPEN_ID)
        setState { copy(openMessageId = null, openSeed = null, openDetail = null) }
    }

    private fun startDetail(id: String) {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            observeDetail(id).collect { detail ->
                setState { copy(openDetail = detail) }
            }
        }
    }

    private fun markRead(id: String) {
        viewModelScope.launch { runCatching { markReadUseCase.invoke(id) } }
    }

    private companion object {
        const val KEY_OPEN_ID = "messages.openMessageId"
    }
}

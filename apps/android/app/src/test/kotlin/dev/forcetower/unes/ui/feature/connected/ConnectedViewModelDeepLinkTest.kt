package dev.forcetower.unes.ui.feature.connected

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedDetail
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessageDetailUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncMessagesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

// A message push can be tapped before the mirror has the message. Like iOS,
// the shell refreshes the inbox once and opens the message only if it is then
// on the device; otherwise the inbox is the floor, never an endless spinner.
internal class ConnectedViewModelDeepLinkTest {
    private val handler = DeepLinkHandler()
    private val observeDetail = mockk<ObserveMessageDetailUseCase>()
    private val syncMessages = mockk<SyncMessagesUseCase>()
    private val viewModel = ConnectedViewModel(
        refreshSession = mockk(relaxed = true),
        backfillMirror = mockk(relaxed = true),
        pingActivity = mockk(relaxed = true),
        observeMessageDetail = observeDetail,
        syncMessages = syncMessages,
        widgetSnapshotPublisher = mockk(relaxed = true),
        analytics = NoOpAnalytics,
        deepLinkHandler = handler,
        inAppUpdater = mockk(relaxed = true),
        reviewPrompter = mockk(relaxed = true),
        logger = Logger,
    )

    @AfterTest
    fun tearDown() = unmockkAll()

    @Test
    fun aMessageAlreadyOnTheDeviceOpensWithoutARefresh() =
        runTest {
            every { observeDetail("m1") } returns flowOf(mockk<MessageFeedDetail>())

            assertEquals(DeepLinkTarget.Message("m1"), open("unes://messages/m1"))
            coVerify(exactly = 0) { syncMessages(any(), any()) }
        }

    @Test
    fun aMessageThatArrivesWithTheRefreshOpens() =
        runTest {
            var mirrored = false
            every { observeDetail("m1") } returns flow { emit(if (mirrored) mockk<MessageFeedDetail>() else null) }
            coEvery { syncMessages(null, null) } answers {
                mirrored = true
                Outcome.Ok(mockk())
            }

            assertEquals(DeepLinkTarget.Message("m1"), open("unes://messages/m1"))
            coVerify(exactly = 1) { syncMessages(null, null) }
        }

    @Test
    fun aMessageStillMissingAfterTheRefreshFallsBackToTheInbox() =
        runTest {
            every { observeDetail("m1") } returns flowOf(null)
            coEvery { syncMessages(null, null) } returns Outcome.Ok(mockk())

            assertEquals(DeepLinkTarget.Tab(ConnectedTab.Messages), open("unes://messages/m1"))
            coVerify(exactly = 1) { syncMessages(null, null) }
        }

    @Test
    fun aFailedRefreshFallsBackToTheInbox() =
        runTest {
            every { observeDetail("m1") } returns flowOf(null)
            coEvery { syncMessages(null, null) } returns Outcome.Err(SyncError.NoConnection)

            assertEquals(DeepLinkTarget.Tab(ConnectedTab.Messages), open("unes://messages/m1"))
        }

    @Test
    fun otherTargetsPassThroughWithoutTouchingTheInbox() =
        runTest {
            assertEquals(DeepLinkTarget.Calendar, open("unes://calendar"))
            assertEquals(DeepLinkTarget.MaterialDetail("mat-1"), open("unes://materials/mat-1"))
            coVerify(exactly = 0) { syncMessages(any(), any()) }
        }

    private suspend fun open(url: String): DeepLinkTarget {
        handler.offer(url)
        return viewModel.deepLinks.first()
    }
}

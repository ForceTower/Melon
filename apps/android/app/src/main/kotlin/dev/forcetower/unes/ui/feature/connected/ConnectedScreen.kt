package dev.forcetower.unes.ui.feature.connected

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.feature.disciplinedetail.DisciplineDetailRoute
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplinesIntent
import dev.forcetower.unes.ui.feature.disciplines.DisciplinesListViewModel
import dev.forcetower.unes.ui.feature.disciplines.DisciplinesScreen
import dev.forcetower.unes.ui.feature.me.MeScreen
import dev.forcetower.unes.ui.feature.messages.MessageDetailRoute
import dev.forcetower.unes.ui.feature.messages.MessagesIntent
import dev.forcetower.unes.ui.feature.messages.MessagesScreen
import dev.forcetower.unes.ui.feature.messages.MessagesViewModel
import dev.forcetower.unes.ui.feature.overview.OverviewScreen
import dev.forcetower.unes.ui.feature.schedule.ScheduleScreen
import dev.forcetower.unes.ui.feature.calendar.CalendarScreen
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownScreen
import dev.forcetower.unes.ui.feature.licenses.LicensesScreen
import dev.forcetower.unes.ui.feature.settings.SettingsScreen

// The authenticated shell — hosts the liquid tab bar and routes to each
// feature's first screen. Mirrors iOS `ConnectedView` in shape: enum-driven
// tabs, single shared chrome, content swapped underneath.
//
// Navigation is per-tab: each tab keeps its own `NavBackStack` (see
// `ConnectedNavigator`), so deep navigation inside a tab (e.g. opening a
// message detail) is popped by the system back gesture without leaving the
// tab. The floating tab bar stays composed across all routes.
@Composable
fun ConnectedScreen(
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenFolioRunner: () -> Unit = {},
) {
    val vm: ConnectedViewModel = hiltViewModel()
    // Fires on initial mount AND on every background → foreground transition,
    // mirroring iOS's `.task` + `.onChange(of: scenePhase)` pair. Concurrent
    // calls are deduped by a Mutex inside `ConnectedViewModel`.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.onAppeared() }

    val navigator = rememberConnectedNavigator(initial = ConnectedTab.Overview)
    // Live unread count piggybacks on `MessagesViewModel`. Without nav3
    // viewmodel-decoration, `hiltViewModel()` resolves to the activity
    // store, so this instance is shared with the one inside the Messages
    // tab — the second `collectAsStateWithLifecycle` is essentially free.
    val messagesVm: MessagesViewModel = hiltViewModel()
    val messagesState by messagesVm.state.collectAsStateWithLifecycle()
    // Same shared-VM trick as messages: the disciplines list VM holds the
    // tap seed, so the detail route can read it without an extra navigation
    // payload (which Nav3 would have to serialize).
    val disciplinesVm: DisciplinesListViewModel = hiltViewModel()
    val unreadBadges = mapOf(
        ConnectedTab.Messages to 0 /* messagesState.rawItems.count { it.isUnread } */,
    ).filterValues { it > 0 }
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = TabBarBlockHeight + navBarBottom

    // Backdrop captured into an offscreen layer with `BlurEffect` set so the
    // tab bar can replay it pre-blurred — Compose's native equivalent of CSS
    // `backdrop-filter: blur(...)`. Mesh.kt uses the same blur primitive (via
    // `Modifier.blur` — content blur). Both rely on API 31+; on older devices
    // the layer draws unblurred, leaving just the translucent chrome.
    val backdrop: GraphicsLayer? = if (BackdropBlurSupported) {
        rememberGraphicsLayer().also { layer ->
            layer.renderEffect = BlurEffect(BackdropBlurRadius, BackdropBlurRadius, TileMode.Decal)
        }
    } else null

    // Lifted above the per-tab decorated entries below so saveable state
    // (lazy scroll positions, expanded/collapsed flags) survives tab
    // switches even though inactive tabs leave composition.
    val saveableDecorator = rememberSaveableStateHolderNavEntryDecorator<NavKey>()

    val entriesByTab = navigator.stacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(saveableDecorator),
            entryProvider = entryProvider {
                entry<ConnectedRoute.Overview>(metadata = TabRootMetadata) {
                    OverviewScreen(
                        bottomInset = bottomInset,
                        onOpenDiscipline = { d ->
                            val offerId = d.offerId ?: return@OverviewScreen
                            disciplinesVm.onIntent(
                                DisciplinesIntent.OpenDiscipline(
                                    seedDiscipline(
                                        code = d.code,
                                        title = d.title,
                                        prof = "",
                                        color = d.color,
                                        offerId = offerId,
                                    ),
                                ),
                            )
                            navigator.navigate(ConnectedRoute.DisciplineDetail(offerId))
                        },
                        onOpenMessages = { navigator.selectTab(ConnectedTab.Messages) },
                        onOpenSchedule = { navigator.selectTab(ConnectedTab.Schedule) },
                    )
                }
                entry<ConnectedRoute.Schedule>(metadata = TabRootMetadata) {
                    ScheduleScreen(
                        bottomInset = bottomInset,
                        onOpenDiscipline = { c ->
                            val offerId = c.offerId ?: return@ScheduleScreen
                            disciplinesVm.onIntent(
                                DisciplinesIntent.OpenDiscipline(
                                    seedDiscipline(
                                        code = c.code,
                                        title = c.title,
                                        prof = c.prof,
                                        color = c.color,
                                        offerId = offerId,
                                    ),
                                ),
                            )
                            navigator.navigate(ConnectedRoute.DisciplineDetail(offerId))
                        },
                        onOpenFolioRunner = onOpenFolioRunner,
                    )
                }
                entry<ConnectedRoute.Classes>(metadata = TabRootMetadata) {
                    DisciplinesScreen(
                        bottomInset = bottomInset,
                        onOpenDiscipline = { d ->
                            val offerId = d.offerId ?: return@DisciplinesScreen
                            disciplinesVm.onIntent(DisciplinesIntent.OpenDiscipline(d))
                            navigator.navigate(ConnectedRoute.DisciplineDetail(offerId))
                        },
                    )
                }
                entry<ConnectedRoute.DisciplineDetail> { route ->
                    DisciplineDetailRoute(
                        offerId = route.offerId,
                        listVm = disciplinesVm,
                        onBack = { navigator.goBack() },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.MessagesList>(metadata = TabRootMetadata) {
                    MessagesScreen(
                        bottomInset = bottomInset,
                        onOpen = { id, seed ->
                            messagesVm.onIntent(MessagesIntent.OpenMessage(id, seed))
                            navigator.navigate(ConnectedRoute.MessageDetail(id))
                        },
                    )
                }
                entry<ConnectedRoute.MessageDetail> { route ->
                    MessageDetailRoute(
                        id = route.id,
                        vm = messagesVm,
                        onBack = { navigator.goBack() },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.Me>(metadata = TabRootMetadata) {
                    MeScreen(onLoggedOut = onLoggedOut, bottomInset = bottomInset)
                }
                entry<ConnectedRoute.Settings> {
                    SettingsScreen(
                        onBack = { navigator.goBack() },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.Calendar> {
                    CalendarScreen(
                        onBack = { navigator.goBack() },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.FinalCountdown> {
                    FinalCountdownScreen(
                        onBack = { navigator.goBack() },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.Licenses> {
                    LicensesScreen(
                        onBack = { navigator.goBack() },
                        bottomInset = bottomInset,
                    )
                }
            },
        )
    }

    CompositionLocalProvider(LocalConnectedNavigator provides navigator) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (backdrop != null) {
                            Modifier.drawWithContent {
                                // Record the (sharp) screen content into the layer
                                // — the layer applies its blur on playback when
                                // the tab bar later calls `drawLayer(backdrop)`.
                                backdrop.record { this@drawWithContent.drawContent() }
                                drawContent()
                            }
                        } else Modifier,
                    ),
            ) {
                NavDisplay(
                    entries = entriesByTab.getValue(navigator.activeTab),
                    onBack = { navigator.goBack() },
                    transitionSpec = connectedPushTransition,
                    popTransitionSpec = connectedPopTransition,
                    predictivePopTransitionSpec = connectedPredictivePopTransition,
                )
            }

            LiquidTabBar(
                items = ConnectedTab.entries,
                active = navigator.activeTab,
                onChange = { navigator.selectTab(it) },
                badges = unreadBadges,
                backdrop = backdrop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Nav bars handle the gesture/3-button bar (any rotation);
                    // horizontal display cutout keeps the bar off the notch when
                    // the device is rotated to landscape. IME is intentionally
                    // excluded so the bar stays under the keyboard.
                    .windowInsetsPadding(
                        WindowInsets.navigationBars
                            .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
                    )
                    .padding(horizontal = 14.dp, vertical = 22.dp),
            )
        }
    }
}

// Minimal seed handed to `DisciplinesListViewModel` when an Overview/Schedule
// tap pushes `DisciplineDetail`. The detail VM hydrates the full payload from
// KMP via `offerId`; the seed only powers the first frame so the screen has
// something to render before the flow emits. Mirrors iOS `detailSeed` in
// `DisciplinesStrip` / `DayColumn`.
private fun seedDiscipline(
    code: String,
    title: String,
    prof: String,
    color: Color,
    offerId: String,
): Discipline = Discipline(
    code = code,
    fullCode = code,
    title = title,
    dept = "",
    prof = prof,
    color = color,
    hours = 0,
    absences = 0,
    allowedAbsences = 0,
    sections = emptyList(),
    offerId = offerId,
)

// Visual height of the floating tab bar block (bar + 22dp top/bottom design
// margin). Content scroll views add this plus the live navigation-bar inset
// to keep the last item from sliding under the bar on devices with a
// 3-button nav bar.
private val TabBarBlockHeight = 110.dp

// 40px Gaussian σ — same order of magnitude as the JSX prototype's
// `backdrop-filter: blur(20px) saturate(180%)` (Compose's BlurEffect uses
// pixel radii and reads heavier than CSS blur for an equivalent perceived
// blur, hence 40 vs 20).
private const val BackdropBlurRadius = 40f
private val BackdropBlurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
private fun ComingSoonPanel(tab: ConnectedTab) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview
@Composable
private fun ConnectedScreenPreview() {
    MelonTheme { ConnectedScreen(onLoggedOut = {}) }
}

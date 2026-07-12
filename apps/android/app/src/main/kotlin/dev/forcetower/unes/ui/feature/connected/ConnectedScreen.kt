package dev.forcetower.unes.ui.feature.connected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import dev.forcetower.unes.designsystem.theme.melon
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
import dev.forcetower.unes.ui.feature.overview.ColorFor
import dev.forcetower.unes.ui.feature.overview.OverviewScreen
import dev.forcetower.unes.ui.feature.schedule.ScheduleScreen
import dev.forcetower.unes.ui.feature.calendar.CalendarScreen
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoExploreKind
import dev.forcetower.unes.ui.feature.finalcountdown.FinalCountdownScreen
import dev.forcetower.unes.ui.feature.licenses.LicensesScreen
import dev.forcetower.unes.ui.feature.materials.MaterialsDetailIntent
import dev.forcetower.unes.ui.feature.materials.MaterialsDetailScreen
import dev.forcetower.unes.ui.feature.materials.MaterialsDetailViewModel
import dev.forcetower.unes.ui.feature.materials.MaterialsHubScreen
import dev.forcetower.unes.ui.feature.materials.MaterialsListScreen
import dev.forcetower.unes.ui.feature.materials.MaterialsSavedScreen
import dev.forcetower.unes.ui.feature.paradoxo.ParadoxoDisciplineScreen
import dev.forcetower.unes.ui.feature.paradoxo.ParadoxoExploreScreen
import dev.forcetower.unes.ui.feature.paradoxo.ParadoxoScreen
import dev.forcetower.unes.ui.feature.paradoxo.ParadoxoTeacherScreen
import dev.forcetower.unes.ui.feature.settings.SettingsScreen

// The authenticated shell — hosts the Material 3 bottom navigation bar and
// routes to each feature's first screen.
//
// Navigation is per-tab: each tab keeps its own `NavBackStack` (see
// `ConnectedNavigator`), so deep navigation inside a tab (e.g. opening a
// message detail) is popped by the system back gesture without leaving the
// tab. The bar stays composed across all routes.
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
    // Materiais is online-only with no per-material fetch endpoint, so the
    // detail seed rides the same shared-VM channel.
    val materialsDetailVm: MaterialsDetailViewModel = hiltViewModel()
    val unreadBadges = mapOf(
        ConnectedTab.Messages to messagesState.rawItems.count { it.isUnread },
    ).filterValues { it > 0 }

    // Captured in composition so tap callbacks (which run outside it) can
    // resolve the stable per-discipline tint for detail seeds.
    val palette = MaterialTheme.melon.palette

    // Content sits above the opaque bar, so screens no longer need to reserve
    // scroll space for floating chrome.
    val bottomInset = 0.dp

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
                                        color = ColorFor.discipline(palette, d.code),
                                        offerId = offerId,
                                    ),
                                ),
                            )
                            navigator.navigate(ConnectedRoute.DisciplineDetail(offerId))
                        },
                        onOpenMessages = { navigator.selectTab(ConnectedTab.Messages) },
                        onOpenSchedule = { navigator.selectTab(ConnectedTab.Schedule) },
                        onOpenProfile = { navigator.selectTab(ConnectedTab.Me) },
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
                entry<ConnectedRoute.FinalCountdown> { route ->
                    FinalCountdownScreen(
                        onBack = { navigator.goBack() },
                        offerId = route.offerId,
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.Licenses> {
                    LicensesScreen(
                        onBack = { navigator.goBack() },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.Paradoxo> {
                    ParadoxoScreen(
                        onBack = { navigator.goBack() },
                        onOpenDiscipline = { id, name ->
                            navigator.navigate(
                                ConnectedRoute.ParadoxoDiscipline(id, name.ifBlank { null }),
                            )
                        },
                        onOpenTeacher = { id, name ->
                            navigator.navigate(
                                ConnectedRoute.ParadoxoTeacher(id, name.ifBlank { null }),
                            )
                        },
                        onOpenExplore = { kind ->
                            navigator.navigate(ConnectedRoute.ParadoxoExplore(kind.name))
                        },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.ParadoxoDiscipline> { route ->
                    ParadoxoDisciplineScreen(
                        id = route.id,
                        seedName = route.name,
                        onBack = { navigator.goBack() },
                        onOpenTeacher = { id, name ->
                            navigator.navigate(ConnectedRoute.ParadoxoTeacher(id, name))
                        },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.ParadoxoTeacher> { route ->
                    ParadoxoTeacherScreen(
                        id = route.id,
                        seedName = route.name,
                        onBack = { navigator.goBack() },
                        onOpenDiscipline = { id, name ->
                            navigator.navigate(ConnectedRoute.ParadoxoDiscipline(id, name))
                        },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.ParadoxoExplore> { route ->
                    ParadoxoExploreScreen(
                        kind = runCatching { ParadoxoExploreKind.valueOf(route.kind) }
                            .getOrDefault(ParadoxoExploreKind.Brutal),
                        onBack = { navigator.goBack() },
                        onOpenDiscipline = { id, name ->
                            navigator.navigate(ConnectedRoute.ParadoxoDiscipline(id, name))
                        },
                        onOpenTeacher = { id, name ->
                            navigator.navigate(ConnectedRoute.ParadoxoTeacher(id, name))
                        },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.Materials> {
                    MaterialsHubScreen(
                        onBack = { navigator.goBack() },
                        onOpenDiscipline = { discipline ->
                            navigator.navigate(
                                ConnectedRoute.MaterialsDiscipline(
                                    disciplineId = discipline.id,
                                    code = discipline.code,
                                    name = discipline.name,
                                ),
                            )
                        },
                        onOpenSaved = { navigator.navigate(ConnectedRoute.MaterialsSaved) },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.MaterialsDiscipline> { route ->
                    MaterialsListScreen(
                        disciplineId = route.disciplineId,
                        seedCode = route.code,
                        seedName = route.name,
                        onBack = { navigator.goBack() },
                        onOpenMaterial = { material ->
                            materialsDetailVm.onIntent(MaterialsDetailIntent.Seed(material))
                            navigator.navigate(
                                ConnectedRoute.MaterialsDetail(
                                    materialId = material.id,
                                    disciplineId = material.discipline.id,
                                ),
                            )
                        },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.MaterialsDetail> { route ->
                    MaterialsDetailScreen(
                        materialId = route.materialId,
                        disciplineId = route.disciplineId,
                        onBack = { navigator.goBack() },
                        bottomInset = bottomInset,
                    )
                }
                entry<ConnectedRoute.MaterialsSaved> {
                    MaterialsSavedScreen(
                        onBack = { navigator.goBack() },
                        onOpenMaterial = { material ->
                            materialsDetailVm.onIntent(MaterialsDetailIntent.Seed(material))
                            navigator.navigate(
                                ConnectedRoute.MaterialsDetail(
                                    materialId = material.id,
                                    disciplineId = material.discipline.id,
                                ),
                            )
                        },
                        bottomInset = bottomInset,
                    )
                }
            },
        )
    }

    CompositionLocalProvider(LocalConnectedNavigator provides navigator) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    entries = entriesByTab.getValue(navigator.activeTab),
                    onBack = { navigator.goBack() },
                    transitionSpec = connectedPushTransition,
                    popTransitionSpec = connectedPopTransition,
                    predictivePopTransitionSpec = connectedPredictivePopTransition,
                )
            }
            ConnectedNavigationBar(
                active = navigator.activeTab,
                onChange = { navigator.selectTab(it) },
                badges = unreadBadges,
            )
        }
    }
}

// Minimal seed handed to `DisciplinesListViewModel` when an Overview/Schedule
// tap pushes `DisciplineDetail`. The detail VM hydrates the full payload from
// KMP via `offerId`; the seed only powers the first frame so the screen has
// something to render before the flow emits.
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

@Preview
@Composable
private fun ConnectedScreenPreview() {
    MelonTheme { ConnectedScreen(onLoggedOut = {}) }
}

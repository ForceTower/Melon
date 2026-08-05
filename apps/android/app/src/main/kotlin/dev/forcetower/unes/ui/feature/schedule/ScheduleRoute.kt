package dev.forcetower.unes.ui.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.ui.feature.schedule.grid.ScheduleGridScreen

// Entry point for the Horário tab: renders whichever rendering the user
// picked in Configurações — the day timeline (`ScheduleScreen`) or the week
// grid (`ScheduleGridScreen`). The choice is observed from
// `SchedulePreferenceStore` through the shared `ScheduleViewModel`, so a
// toggle flip in Configurações swaps the tab on the next visit with no
// restart.
@Composable
internal fun ScheduleRoute(
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
    onOpenDiscipline: (ScheduleClass) -> Unit = {},
    onOpenMaterials: (ScheduleClass) -> Unit = {},
    onOpenFolioRunner: () -> Unit = {},
) {
    val vm: ScheduleViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    when (state.gridEnabled) {
        true -> ScheduleGridScreen(
            modifier = modifier,
            bottomInset = bottomInset,
            onOpenDiscipline = onOpenDiscipline,
            onOpenFolioRunner = onOpenFolioRunner,
        )
        // The grid rendering surfaces its actions through the detail sheet,
        // which carries no Materiais entry — only the timeline needs it.
        false -> ScheduleScreen(
            modifier = modifier,
            bottomInset = bottomInset,
            onOpenDiscipline = onOpenDiscipline,
            onOpenMaterials = onOpenMaterials,
            onOpenFolioRunner = onOpenFolioRunner,
        )
        // Preference not read yet — hold a blank background for the frame or
        // two before DataStore emits, instead of mounting the wrong screen.
        null -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
    }
}

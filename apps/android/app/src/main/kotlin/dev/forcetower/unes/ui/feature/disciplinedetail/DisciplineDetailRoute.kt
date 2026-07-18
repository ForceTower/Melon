package dev.forcetower.unes.ui.feature.disciplinedetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.DisciplinesIntent
import dev.forcetower.unes.ui.feature.disciplines.DisciplinesListViewModel
import dev.forcetower.unes.ui.feature.overview.ColorFor

// Adapter sitting between the Classes tab's `DisciplineDetail` NavKey and the
// pure detail screen. Owns the open/close lifecycle of the detail subscription
// on `DisciplineDetailViewModel`: opens on enter (passing the seed Discipline
// from the list VM), closes on dispose. Mirrors `MessageDetailRoute` —
// shared parent VM holds the seed, scoped child VM owns the hydration flow.
@Composable
internal fun DisciplineDetailRoute(
    offerId: String,
    listVm: DisciplinesListViewModel,
    onBack: () -> Unit,
    onOpenMaterials: (disciplineId: String, code: String, name: String) -> Unit,
    bottomInset: Dp = 0.dp,
) {
    val detailVm: DisciplineDetailViewModel = hiltViewModel()
    val listState by listVm.state.collectAsStateWithLifecycle()
    val detailState by detailVm.state.collectAsStateWithLifecycle()

    // Pull the seed out of the parent VM (set when the list-card was tapped).
    // Falls back through `openSeed` → list scan so the screen still renders
    // when the route is restored after process death.
    val seed: Discipline? = remember(offerId, listState) {
        listState.openSeed?.takeIf { it.offerId == offerId }
            ?: listState.current?.disciplines?.firstOrNull { it.offerId == offerId }
            ?: listState.past
                .asSequence()
                .flatMap { it.disciplines.asSequence() }
                .firstOrNull { it.offerId == offerId }
    }

    LaunchedEffect(offerId, seed) {
        // Re-issuing Open with the same id is a no-op for the subscription
        // (the VM dedupes); it just refreshes the seed if a newer one arrived.
        detailVm.onIntent(DisciplineDetailIntent.Open(offerId, seed))
    }

    DisposableEffect(offerId) {
        onDispose {
            // Tear down the detail subscription when this entry leaves
            // composition (popped or tab-switched away). The seed handover on
            // the parent VM also gets cleared so the next open doesn't reuse a
            // stale Discipline.
            if (detailVm.state.value.offerId == offerId) {
                detailVm.onIntent(DisciplineDetailIntent.Close)
            }
            listVm.onIntent(DisciplinesIntent.CloseDiscipline)
        }
    }

    val rendered = detailState.discipline?.let { tinted(it) } ?: return
    val disciplineId = rendered.disciplineId
    DisciplineDetailScreen(
        discipline = rendered,
        selectedGroup = detailState.selectedGroup,
        onSelectGroup = { detailVm.onIntent(DisciplineDetailIntent.SelectGroup(it)) },
        onBack = onBack,
        collabCount = detailState.collabCount,
        onOpenMaterials = disciplineId?.let { id ->
            {
                detailVm.trackOpenMaterials(id)
                onOpenMaterials(id, rendered.code, rendered.title)
            }
        },
        bottomInset = bottomInset,
    )
}

// Resolve the discipline color against the live palette. The list-tinted seed
// already carries a real color, but a freshly-mapped detail emission lands
// with `Color.Unspecified` (the mapper has no Composable context). Tint here
// so the screen always paints with the current theme's palette slot.
@Composable
private fun tinted(d: Discipline): Discipline {
    val resolved = ColorFor.discipline(d.code)
    return if (d.color == androidx.compose.ui.graphics.Color.Unspecified) {
        d.copy(color = resolved)
    } else {
        d
    }
}

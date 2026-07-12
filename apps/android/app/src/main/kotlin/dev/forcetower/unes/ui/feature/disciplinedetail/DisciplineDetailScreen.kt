package dev.forcetower.unes.ui.feature.disciplinedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineCargaCard
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineCollabBanner
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineDetailHeader
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineDetailTabs
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineEmentaCard
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineFinalsCallout
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineFrequenciaCard
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineMediaHero
import dev.forcetower.unes.ui.feature.disciplinedetail.components.DisciplineNotasCard
import dev.forcetower.unes.ui.feature.disciplines.Discipline
import dev.forcetower.unes.ui.feature.disciplines.isAwaitingFinal
import java.util.Locale

// Detalhe da disciplina — the dc `DisciplineDetailScreen` as one scroll: M3
// top bar, headline (code chip / teachers / group filter), média-parcial hero,
// Notas list with the Prova Final section, carga horária, the finals callout,
// frequência, ementa, the Colaborativo banner into Materiais, and the
// Materiais/Aulas secondary tabs.
@Composable
internal fun DisciplineDetailScreen(
    discipline: Discipline,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    collabCount: Int? = null,
    onOpenMaterials: (() -> Unit)? = null,
    bottomInset: Dp = 0.dp,
) {
    val listState = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    // The top bar stays pinned; the headline and the cards scroll beneath it.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        TopBar(onBack = onBack)
        PinnedHeaderHairline(scrolled = scrolled)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
        ) {
            item("header") {
                DisciplineDetailHeader(
                    discipline = discipline,
                    selectedGroup = selectedGroup,
                    onSelectGroup = onSelectGroup,
                    modifier = Modifier.fadeUpOnAppear(),
                )
            }
            item("hero") {
                DisciplineMediaHero(
                    discipline = discipline,
                    modifier = Modifier
                        .fadeUpOnAppear(delayMs = 60)
                        .padding(horizontal = 16.dp)
                        .padding(top = 18.dp),
                )
            }
            item("notas") {
                DisciplineNotasCard(
                    discipline = discipline,
                    selectedGroup = selectedGroup,
                    modifier = Modifier
                        .fadeUpOnAppear(delayMs = 120)
                        .padding(horizontal = 16.dp)
                        .padding(top = 18.dp),
                )
            }
            item("carga") {
                DisciplineCargaCard(
                    discipline = discipline,
                    modifier = Modifier
                        .fadeUpOnAppear(delayMs = 160)
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp),
                )
            }
            if (discipline.isAwaitingFinal) {
                item("callout") {
                    DisciplineFinalsCallout(
                        discipline = discipline,
                        modifier = Modifier
                            .fadeUpOnAppear(delayMs = 200)
                            .padding(horizontal = 16.dp)
                            .padding(top = 14.dp),
                    )
                }
            }
            item("frequencia") {
                SectionOverline(
                    text = stringResource(R.string.discipline_detail_freq_title),
                    modifier = Modifier.padding(top = 18.dp),
                )
                DisciplineFrequenciaCard(
                    discipline = discipline,
                    modifier = Modifier
                        .fadeUpOnAppear(delayMs = 240)
                        .padding(horizontal = 16.dp),
                )
            }
            if (!discipline.ementa.isNullOrEmpty()) {
                item("ementa") {
                    DisciplineEmentaCard(
                        discipline = discipline,
                        modifier = Modifier
                            .fadeUpOnAppear(delayMs = 280)
                            .padding(horizontal = 16.dp)
                            .padding(top = 14.dp),
                    )
                }
            }
            if (collabCount != null && onOpenMaterials != null) {
                item("colaborativo") {
                    DisciplineCollabBanner(
                        discipline = discipline,
                        count = collabCount,
                        onOpen = onOpenMaterials,
                        modifier = Modifier
                            .fadeUpOnAppear(delayMs = 320)
                            .padding(horizontal = 16.dp)
                            .padding(top = 14.dp),
                    )
                }
            }
            item("tabs") {
                DisciplineDetailTabs(
                    discipline = discipline,
                    selectedGroup = selectedGroup,
                    modifier = Modifier
                        .fadeUpOnAppear(delayMs = 360)
                        .padding(top = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.discipline_detail_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.discipline_detail_topbar_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionOverline(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(horizontal = 20.dp, vertical = 0.dp)) {
        Text(
            text = text.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
}

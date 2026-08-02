package dev.forcetower.unes.ui.feature.library.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetGroup
import dev.forcetower.melon.feature.library.domain.model.LibrarySort
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.library.LibrarySearchSession
import dev.forcetower.unes.ui.feature.library.components.LibraryInfoNote
import dev.forcetower.unes.ui.feature.library.facetValueLabel
import dev.forcetower.unes.ui.feature.library.formatLibraryCount
import dev.forcetower.unes.ui.feature.library.labelRes

// "Refinar" bottom sheet (dc `BibliotecaScreen` refine sheet, M3-native):
// sort radios, the availability/grouping switches, then each server facet
// group as a collapsible checkbox list with its counts. Every change applies
// immediately (the pager rebuilds behind the sheet); the CTA just closes.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryRefineSheet(
    session: LibrarySearchSession,
    onDismiss: () -> Unit,
    onSetSort: (LibrarySort) -> Unit,
    onToggleFacet: (LibraryFacetGroup, String) -> Unit,
    onClearFacets: () -> Unit,
    onSetOnlyAvailable: (Boolean) -> Unit,
    onSetGroupByType: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var openGroups by rememberSaveable {
        mutableStateOf(setOf(LibraryFacetGroup.Type.wire, LibraryFacetGroup.Branch.wire))
    }
    var expandedGroups by rememberSaveable { mutableStateOf(setOf<String>()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.library_refine_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.44).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClearFacets, enabled = session.activeFacetCount > 0) {
                    Text(
                        text = stringResource(R.string.library_filters_clear),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.library_refine_sort_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                )
                LibrarySort.entries.forEach { sort ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onSetSort(sort) })
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = session.sort == sort,
                            onClick = { onSetSort(sort) },
                        )
                        Text(
                            text = stringResource(sort.labelRes()),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = if (session.sort == sort) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Medium
                                },
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

                SwitchRow(
                    title = stringResource(R.string.library_refine_only_available),
                    subtitle = stringResource(R.string.library_refine_only_available_hint),
                    checked = session.onlyAvailable,
                    onToggle = onSetOnlyAvailable,
                )
                SwitchRow(
                    title = stringResource(R.string.library_refine_group),
                    subtitle = stringResource(R.string.library_refine_group_hint),
                    checked = session.groupByType,
                    onToggle = onSetGroupByType,
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))

                LibraryFacetGroup.entries.forEach { group ->
                    val values = session.serverFacets[group].orEmpty()
                    if (values.isEmpty()) return@forEach
                    val open = group.wire in openGroups
                    val expanded = group.wire in expandedGroups
                    val selectedCount = session.facets[group]?.size ?: 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                openGroups = if (open) {
                                    openGroups - group.wire
                                } else {
                                    openGroups + group.wire
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    ) {
                        Text(
                            text = stringResource(group.labelRes()),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.16).sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (selectedCount > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(start = 12.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            ) {
                                Text(
                                    text = selectedCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(if (open) 180f else 0f),
                        )
                    }
                    if (open) {
                        val visible = if (expanded) values else values.take(5)
                        visible.forEach { value ->
                            val checked = session.facets[group]?.contains(value.key) == true
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleFacet(group, value.key) }
                                    .padding(horizontal = 16.dp),
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { onToggleFacet(group, value.key) },
                                )
                                Text(
                                    text = facetValueLabel(group, value),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (checked) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Medium
                                        },
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = formatLibraryCount(value.count),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.5.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                        if (!expanded && values.size > 5) {
                            TextButton(
                                onClick = { expandedGroups = expandedGroups + group.wire },
                                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.library_refine_show_all,
                                        values.size,
                                    ),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    LibraryInfoNote(
                        text = stringResource(R.string.library_refine_counts_note),
                        icon = Icons.Filled.Info,
                    )
                }
            }

            HorizontalDivider()
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                val total = session.total
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = if (total != null) {
                            pluralStringResource(
                                R.plurals.library_refine_apply,
                                total,
                                formatLibraryCount(total),
                            )
                        } else {
                            stringResource(R.string.library_refine_apply_fallback)
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 24.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                ),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 3.dp, end = 12.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

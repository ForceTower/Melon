package dev.forcetower.unes.ui.feature.library.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.library.domain.model.LibraryBranch
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetGroup
import dev.forcetower.melon.feature.library.domain.model.LibraryFacetSelection
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchOperator
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchScope
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchTerm
import dev.forcetower.melon.feature.library.domain.model.LibraryWorkType
import dev.forcetower.unes.R
import dev.forcetower.unes.ui.feature.library.components.LibraryFilterChip
import dev.forcetower.unes.ui.feature.library.labelRes
import dev.forcetower.unes.ui.feature.library.pluralLabelRes

// "Busca avançada" bottom sheet (dc `BibliotecaScreen` advanced sheet):
// up to three scoped terms joined by E / OU / NÃO (M3 segmented buttons +
// outlined fields with the scope as floating label), plus the "restringir de
// saída" chips that seed a branch/type facet before the search even runs —
// narrowing upstream is what dodges Pergamum's 30s timeout.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryAdvancedSheet(
    initialQuery: String,
    onDismiss: () -> Unit,
    onSubmit: (terms: List<LibrarySearchTerm>, initialFacets: LibraryFacetSelection) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    data class TermRow(
        val query: String,
        val scope: LibrarySearchScope,
        val op: LibrarySearchOperator,
    )

    var rows by remember {
        mutableStateOf(
            listOf(
                TermRow(initialQuery, LibrarySearchScope.Title, LibrarySearchOperator.And),
                TermRow("", LibrarySearchScope.Author, LibrarySearchOperator.And),
            ),
        )
    }
    var branch by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf<String?>(null) }

    val filled = rows.filter { it.query.isNotBlank() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.library_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = stringResource(R.string.library_advanced_entry_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { rows = rows.map { it.copy(query = "") } }) {
                    Text(
                        text = stringResource(R.string.library_advanced_clear),
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
                rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = if (index == 1) 0.dp else 14.dp, bottom = 14.dp),
                        ) {
                            LibrarySearchOperator.entries.forEachIndexed { opIndex, op ->
                                SegmentedButton(
                                    selected = row.op == op,
                                    onClick = {
                                        rows = rows.mapIndexed { i, r ->
                                            if (i == index) r.copy(op = op) else r
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = opIndex,
                                        count = LibrarySearchOperator.entries.size,
                                    ),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = MaterialTheme.colorScheme.primary
                                            .copy(alpha = 0.18f),
                                        activeContentColor = MaterialTheme.colorScheme.primary,
                                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                ) {
                                    Text(
                                        text = stringResource(
                                            when (op) {
                                                LibrarySearchOperator.And ->
                                                    R.string.library_advanced_op_and
                                                LibrarySearchOperator.Or ->
                                                    R.string.library_advanced_op_or
                                                LibrarySearchOperator.Not ->
                                                    R.string.library_advanced_op_not
                                            },
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = row.query,
                        onValueChange = { value ->
                            rows = rows.mapIndexed { i, r ->
                                if (i == index) r.copy(query = value) else r
                            }
                        },
                        label = { Text(stringResource(row.scope.labelRes())) },
                        placeholder = {
                            Text(stringResource(R.string.library_advanced_term_placeholder))
                        },
                        singleLine = true,
                        trailingIcon = if (index > 0) {
                            {
                                IconButton(
                                    onClick = {
                                        rows = rows.filterIndexed { i, _ -> i != index }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(
                                            R.string.library_advanced_remove_term,
                                        ),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 6.dp, bottom = 14.dp),
                    ) {
                        LibrarySearchScope.entries
                            .filter { it != LibrarySearchScope.All }
                            .forEach { scope ->
                                LibraryFilterChip(
                                    selected = row.scope == scope,
                                    onClick = {
                                        rows = rows.mapIndexed { i, r ->
                                            if (i == index) r.copy(scope = scope) else r
                                        }
                                    },
                                    label = stringResource(scope.labelRes()),
                                    showCheck = false,
                                )
                            }
                    }
                }

                if (rows.size < 3) {
                    OutlinedButton(
                        onClick = {
                            rows = rows + TermRow(
                                query = "",
                                scope = LibrarySearchScope.Subject,
                                op = LibrarySearchOperator.And,
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(
                                if (rows.size == 1) {
                                    R.string.library_advanced_add_second
                                } else {
                                    R.string.library_advanced_add_third
                                },
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.library_advanced_restrict_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 26.dp),
                )
                Text(
                    text = stringResource(R.string.library_advanced_restrict_note),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp),
                ) {
                    LibraryBranch.known.forEach { known ->
                        LibraryFilterChip(
                            selected = branch == known.id,
                            onClick = { branch = if (branch == known.id) null else known.id },
                            label = known.sigla,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp, bottom = 16.dp),
                ) {
                    listOf(
                        LibraryWorkType.Book,
                        LibraryWorkType.Dissertation,
                        LibraryWorkType.Cordel,
                        LibraryWorkType.Article,
                    ).forEach { workType ->
                        LibraryFilterChip(
                            selected = type == workType.wire,
                            onClick = {
                                type = if (type == workType.wire) null else workType.wire
                            },
                            label = stringResource(workType.pluralLabelRes()),
                        )
                    }
                }
            }

            HorizontalDivider()
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Button(
                    onClick = {
                        if (filled.isEmpty()) return@Button
                        val facets = buildMap<LibraryFacetGroup, Set<String>> {
                            branch?.let { put(LibraryFacetGroup.Branch, setOf(it)) }
                            type?.let { put(LibraryFacetGroup.Type, setOf(it)) }
                        }
                        onSubmit(
                            filled.map {
                                LibrarySearchTerm(
                                    query = it.query.trim(),
                                    scope = it.scope,
                                    op = it.op,
                                )
                            },
                            facets,
                        )
                    },
                    enabled = filled.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (filled.isEmpty()) {
                            stringResource(R.string.library_advanced_submit_empty)
                        } else {
                            pluralStringResource(
                                R.plurals.library_advanced_submit,
                                filled.size,
                                filled.size,
                            )
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

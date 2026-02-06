package com.forcetower.uefs.feature.enrollment.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CatalogCourseItem
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CatalogIntent
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CatalogState
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CatalogViewModel
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CourseGroupDetails
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CourseIcon
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.CourseType
import com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel.DepartmentFilter
import com.forcetower.uefs.ui.arch.ObserveEvent
import com.forcetower.uefs.ui.theme.MelonTheme

private val MandatoryColor = Color(0xFF1565C0)
private val ElectiveColor = Color(0xFF2E7D32)
private val ConflictColor = Color(0xFFE53935)
private val ConflictBackgroundColor = Color(0xFFFFF3F3)
private val SelectedBackgroundColor = Color(0xFFF0F4FF)
private val SaveButtonColor = Color(0xFF1565C0)

@Composable
internal fun Catalog(
    viewModel: CatalogViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveEvent(viewModel.event) {
    }

    CatalogContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
private fun CatalogContent(
    state: CatalogState,
    onIntent: (CatalogIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CatalogTopBar(
                searchQuery = state.searchQuery,
                departments = state.departments,
                onIntent = onIntent
            )
        },
        bottomBar = {
            CatalogFooter(
                semesterLabel = state.semesterLabel,
                totalCreditsHours = state.totalCreditsHours,
                maxCreditsHours = state.maxCreditsHours,
                selectedCount = state.selectedCount,
                onSave = { onIntent(CatalogIntent.OnSaveEnrollment) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.courses, key = { it.id }) { course ->
                CatalogCourseCard(
                    course = course,
                    onExpandToggle = { onIntent(CatalogIntent.OnCourseExpandToggle(course.id)) },
                    onChangeGroup = { groupIndex ->
                        onIntent(CatalogIntent.OnChangeClassGroup(course.id, groupIndex))
                    },
                    onToggleSelection = { onIntent(CatalogIntent.OnToggleCourseSelection(course.id)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogTopBar(
    searchQuery: String,
    departments: List<DepartmentFilter>,
    onIntent: (CatalogIntent) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column {
            TopAppBar(
                title = { Text("Matrícula") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(CatalogIntent.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(CatalogIntent.OnFilter) }) {
                        Icon(Icons.Filled.Tune, contentDescription = "Filtros")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            CatalogSearchBar(
                query = searchQuery,
                onQueryChanged = { onIntent(CatalogIntent.OnSearchQueryChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (departments.isNotEmpty()) {
                CatalogFilterChips(
                    departments = departments,
                    onDepartmentSelected = { onIntent(CatalogIntent.OnDepartmentSelected(it)) },
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CatalogSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Buscar disciplina...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CatalogFilterChips(
    departments: List<DepartmentFilter>,
    onDepartmentSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(departments, key = { it.id }) { department ->
            FilterChip(
                selected = department.selected,
                onClick = { onDepartmentSelected(department.id) },
                label = {
                    Text(
                        text = department.name,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun CatalogCourseCard(
    course: CatalogCourseItem,
    onExpandToggle: () -> Unit,
    onChangeGroup: (Int) -> Unit,
    onToggleSelection: () -> Unit
) {
    val typeColor = when (course.type) {
        CourseType.MANDATORY -> MandatoryColor
        CourseType.ELECTIVE -> ElectiveColor
    }

    val containerColor = when {
        course.hasConflict -> ConflictBackgroundColor
        course.selected -> SelectedBackgroundColor
        else -> MaterialTheme.colorScheme.surface
    }
    val cardColors = CardDefaults.cardColors(containerColor = containerColor)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = cardColors
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = course.icon.imageVector,
                        contentDescription = course.icon.label,
                        tint = typeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title + metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = course.code,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${course.creditsHours}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = course.type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Expand/collapse icon
                Icon(
                    imageVector = if (course.expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (course.expanded) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Conflict warning
            if (course.hasConflict && course.conflictMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ConflictColor.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = ConflictColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = course.conflictMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = ConflictColor
                    )
                }
            }

            // Expanded details
            AnimatedVisibility(
                visible = course.expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    if (course.groups.isNotEmpty()) {
                        CourseExpandedDetails(
                            groups = course.groups,
                            selectedGroupIndex = course.selectedGroupIndex,
                            onChangeGroup = onChangeGroup,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    if (!course.hasConflict) {
                        Button(
                            onClick = onToggleSelection,
                            colors = ButtonDefaults.buttonColors(containerColor = SaveButtonColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text("Selecionar disciplina")
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun CourseExpandedDetails(
    groups: List<CourseGroupDetails>,
    selectedGroupIndex: Int,
    onChangeGroup: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Group selector (if multiple groups)
        if (groups.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groups.forEachIndexed { index, group ->
                    FilterChip(
                        selected = index == selectedGroupIndex,
                        onClick = { onChangeGroup(index) },
                        label = { Text(group.groupName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        val group = groups.getOrNull(selectedGroupIndex) ?: return

        // Schedule
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = group.schedule,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Professor
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = group.professor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vacancy bar
        VacancyProgressBar(
            enrolledCount = group.enrolledCount,
            totalVacancies = group.totalVacancies
        )
    }
}

@Composable
private fun VacancyProgressBar(
    enrolledCount: Int,
    totalVacancies: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalVacancies > 0) {
        (enrolledCount.toFloat() / totalVacancies).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progressColor = when {
        progress >= 0.9f -> ConflictColor
        progress >= 0.7f -> Color(0xFFF57C00)
        else -> Color(0xFF2E7D32)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Vagas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$enrolledCount / $totalVacancies",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = progressColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun CatalogFooter(
    semesterLabel: String,
    totalCreditsHours: Int,
    maxCreditsHours: Int,
    selectedCount: Int,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (semesterLabel.isNotEmpty()) {
                    Text(
                        text = semesterLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = "$totalCreditsHours / ${maxCreditsHours}h cr\u00e9ditos",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$selectedCount disciplinas selecionadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = SaveButtonColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar")
            }
        }
    }
}

// region Preview

private val previewDepartments = listOf(
    DepartmentFilter(id = "all", name = "Todos", selected = true),
    DepartmentFilter(id = "exa", name = "Ci\u00eancias Exatas"),
    DepartmentFilter(id = "tec", name = "Tecnologia"),
    DepartmentFilter(id = "hum", name = "Humanas"),
    DepartmentFilter(id = "sau", name = "Sa\u00fade"),
)

private val previewCourses = listOf(
    CatalogCourseItem(
        id = "1",
        code = "EXA854",
        name = "C\u00e1lculo Diferencial e Integral II",
        department = "Ci\u00eancias Exatas",
        type = CourseType.MANDATORY,
        icon = CourseIcon.MATH,
        creditsHours = 60,
        expanded = true,
        groups = listOf(
            CourseGroupDetails(
                groupName = "T01",
                schedule = "Seg/Qua 10:00 - 12:00",
                professor = "Dr. Jo\u00e3o Silva",
                enrolledCount = 35,
                totalVacancies = 45
            ),
            CourseGroupDetails(
                groupName = "T02",
                schedule = "Ter/Qui 14:00 - 16:00",
                professor = "Dra. Maria Santos",
                enrolledCount = 42,
                totalVacancies = 45
            )
        ),
        selectedGroupIndex = 0
    ),
    CatalogCourseItem(
        id = "2",
        code = "EXA876",
        name = "Algoritmos e Estruturas de Dados",
        department = "Tecnologia",
        type = CourseType.MANDATORY,
        icon = CourseIcon.COMPUTING,
        creditsHours = 75,
        hasConflict = true,
        conflictMessage = "Conflito de hor\u00e1rio com C\u00e1lculo II",
        groups = listOf(
            CourseGroupDetails(
                groupName = "T01",
                schedule = "Seg/Qua 10:00 - 12:30",
                professor = "Dr. Carlos Mendes",
                enrolledCount = 40,
                totalVacancies = 40
            )
        )
    ),
    CatalogCourseItem(
        id = "3",
        code = "LET501",
        name = "Artes Visuais e Express\u00e3o",
        department = "Humanas",
        type = CourseType.ELECTIVE,
        icon = CourseIcon.ARTS,
        creditsHours = 30,
        groups = listOf(
            CourseGroupDetails(
                groupName = "T01",
                schedule = "Sex 08:00 - 10:00",
                professor = "Dra. Ana Lima",
                enrolledCount = 15,
                totalVacancies = 30
            )
        )
    ),
    CatalogCourseItem(
        id = "4",
        code = "BIO320",
        name = "Ecologia e Meio Ambiente",
        department = "Sa\u00fade",
        type = CourseType.ELECTIVE,
        icon = CourseIcon.NATURE,
        creditsHours = 45,
        groups = listOf(
            CourseGroupDetails(
                groupName = "T01",
                schedule = "Ter/Qui 08:00 - 10:00",
                professor = "Dr. Pedro Rocha",
                enrolledCount = 28,
                totalVacancies = 35
            )
        )
    )
)

@Preview(showBackground = true)
@Composable
private fun CatalogContentPreview() {
    MelonTheme(dynamicColor = false) {
        CatalogContent(
            state = CatalogState(
                loading = false,
                searchQuery = "",
                departments = previewDepartments,
                courses = previewCourses,
                semesterLabel = "2026.1",
                totalCreditsHours = 210,
                maxCreditsHours = 360,
                selectedCount = 4
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogCourseCardExpandedPreview() {
    MelonTheme(dynamicColor = false) {
        CatalogCourseCard(
            course = previewCourses[0].copy(selected = true),
            onExpandToggle = {},
            onChangeGroup = {},
            onToggleSelection = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogCourseCardConflictPreview() {
    MelonTheme(dynamicColor = false) {
        CatalogCourseCard(
            course = previewCourses[1].copy(selected = true),
            onExpandToggle = {},
            onChangeGroup = {},
            onToggleSelection = {}
        )
    }
}

// endregion

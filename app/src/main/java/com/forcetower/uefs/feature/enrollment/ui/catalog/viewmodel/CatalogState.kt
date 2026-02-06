package com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector
import dev.forcetower.breaker.model.enrollment.EnrollmentOffer

internal data class CatalogState(
    val loading: Boolean = true,
    val availableOffers: List<EnrollmentOffer> = emptyList(),
    val searchQuery: String = "",
    val departments: List<DepartmentFilter> = emptyList(),
    val allCourses: List<CatalogCourseItem> = emptyList(),
    val courses: List<CatalogCourseItem> = emptyList(),
    val semesterLabel: String = "",
    val totalCreditsHours: Int = 0,
    val maxCreditsHours: Int = 0,
    val selectedCount: Int = 0
)

internal data class DepartmentFilter(
    val id: String,
    val name: String,
    val selected: Boolean = false
)

internal data class CatalogCourseItem(
    val id: String,
    val code: String,
    val name: String,
    val department: String,
    val type: CourseType,
    val icon: CourseIcon,
    val creditsHours: Int,
    val expanded: Boolean = false,
    val hasConflict: Boolean = false,
    val conflictMessage: String? = null,
    val groups: List<CourseGroupDetails> = emptyList(),
    val selectedGroupIndex: Int = 0
)

internal data class CourseGroupDetails(
    val groupName: String,
    val schedule: String,
    val professor: String,
    val enrolledCount: Int,
    val totalVacancies: Int
)

internal enum class CourseType(val label: String) {
    MANDATORY("Obrigatória"),
    ELECTIVE("Optativa")
}

internal enum class CourseIcon(val imageVector: ImageVector, val label: String) {
    MATH(Icons.Filled.Functions, "Matemática"),
    COMPUTING(Icons.Filled.Calculate, "Computação"),
    ARTS(Icons.Filled.Palette, "Artes"),
    NATURE(Icons.Filled.Eco, "Natureza"),
    GENERAL(Icons.Filled.School, "Geral")
}

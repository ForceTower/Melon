package com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel

internal sealed interface CatalogIntent {
    data object OnNavigateBack : CatalogIntent
    data object OnFilter : CatalogIntent
    data class OnSearchQueryChanged(val query: String) : CatalogIntent
    data class OnDepartmentSelected(val departmentId: String) : CatalogIntent
    data class OnCourseExpandToggle(val courseId: String) : CatalogIntent
    data class OnChangeClassGroup(val courseId: String, val groupIndex: Int) : CatalogIntent
    data class OnToggleCourseSelection(val courseId: String) : CatalogIntent
    data object OnSaveEnrollment : CatalogIntent
}

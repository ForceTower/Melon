package com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel

import com.forcetower.uefs.ui.arch.ArchViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.forcetower.breaker.model.enrollment.EnrollmentOffer
import javax.inject.Inject

@HiltViewModel
internal class CatalogViewModel @Inject constructor() : ArchViewModel<CatalogState, CatalogEvent, CatalogIntent>(CatalogState()) {
    override suspend fun handleIntent(intent: CatalogIntent) {
        when (intent) {
            is CatalogIntent.OnSearchQueryChanged -> onSearchQueryChanged(intent.query)
            is CatalogIntent.OnDepartmentSelected -> onDepartmentSelected(intent.departmentId)
            is CatalogIntent.OnCourseExpandToggle -> onCourseExpandToggle(intent.courseId)
            is CatalogIntent.OnChangeClassGroup -> onChangeClassGroup(intent.courseId, intent.groupIndex)
            is CatalogIntent.OnNavigateBack -> sendEvent { CatalogEvent.NavigateBack }
            is CatalogIntent.OnFilter -> { }
            is CatalogIntent.OnSaveEnrollment -> { }
        }
    }

    fun loadData(offers: List<EnrollmentOffer>) {
        val allCourses = offers.map { it.toCatalogCourseItem() }

        val departmentNames = allCourses.map { it.department }.distinct().sorted()
        val departments = buildList {
            add(DepartmentFilter(id = "all", name = "Todos", selected = true))
            addAll(departmentNames.map { DepartmentFilter(id = it, name = it) })
        }

        setState { state ->
            state.copy(
                loading = false,
                availableOffers = offers,
                allCourses = allCourses,
                courses = allCourses,
                departments = departments
            )
        }
    }

    private fun onSearchQueryChanged(query: String) {
        setState { state ->
            val filtered = filterCourses(state.allCourses, query, state.departments)
            state.copy(searchQuery = query, courses = filtered)
        }
    }

    private fun onDepartmentSelected(departmentId: String) {
        setState { state ->
            val updatedDepartments = if (departmentId == "all") {
                state.departments.map { it.copy(selected = it.id == "all") }
            } else {
                state.departments.map { dept ->
                    when (dept.id) {
                        "all" -> dept.copy(selected = false)
                        departmentId -> dept.copy(selected = !dept.selected)
                        else -> dept
                    }
                }.let { deps ->
                    if (deps.none { it.selected }) {
                        deps.map { it.copy(selected = it.id == "all") }
                    } else {
                        deps
                    }
                }
            }
            val filtered = filterCourses(state.allCourses, state.searchQuery, updatedDepartments)
            state.copy(departments = updatedDepartments, courses = filtered)
        }
    }

    private fun onCourseExpandToggle(courseId: String) {
        setState { state ->
            state.copy(
                courses = state.courses.map { course ->
                    if (course.id == courseId) course.copy(expanded = !course.expanded) else course
                },
                allCourses = state.allCourses.map { course ->
                    if (course.id == courseId) course.copy(expanded = !course.expanded) else course
                }
            )
        }
    }

    private fun onChangeClassGroup(courseId: String, groupIndex: Int) {
        setState { state ->
            state.copy(
                courses = state.courses.map { course ->
                    if (course.id == courseId) course.copy(selectedGroupIndex = groupIndex) else course
                },
                allCourses = state.allCourses.map { course ->
                    if (course.id == courseId) course.copy(selectedGroupIndex = groupIndex) else course
                }
            )
        }
    }

    private fun filterCourses(
        allCourses: List<CatalogCourseItem>,
        searchQuery: String,
        departments: List<DepartmentFilter>
    ): List<CatalogCourseItem> {
        val allSelected = departments.any { it.id == "all" && it.selected }
        val selectedDepartments = departments.filter { it.selected && it.id != "all" }.map { it.name }.toSet()

        return allCourses.filter { course ->
            val matchesSearch = searchQuery.isBlank() ||
                course.name.contains(searchQuery, ignoreCase = true) ||
                course.code.contains(searchQuery, ignoreCase = true)

            val matchesDepartment = allSelected || course.department in selectedDepartments

            matchesSearch && matchesDepartment
        }
    }
}

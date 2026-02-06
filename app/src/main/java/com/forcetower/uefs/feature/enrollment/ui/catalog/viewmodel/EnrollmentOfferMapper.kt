package com.forcetower.uefs.feature.enrollment.ui.catalog.viewmodel

import com.forcetower.uefs.feature.enrollment.data.EnrollmentOffer

internal data class DepartmentInfo(
    val name: String,
    val icon: CourseIcon
)

private val departmentsByPrefix: Map<String, DepartmentInfo> = mapOf(
    // Ciencias Exatas -> MATH
    "EXA" to DepartmentInfo("Ciencias Exatas", CourseIcon.MATH),
    "MCTA" to DepartmentInfo("Ciencias Exatas", CourseIcon.MATH),
    "MA" to DepartmentInfo("Ciencias Exatas", CourseIcon.MATH),
    "PGCC" to DepartmentInfo("Ciencias Exatas", CourseIcon.MATH),
    "CAP" to DepartmentInfo("Ciencias Exatas", CourseIcon.MATH),
    "EMA" to DepartmentInfo("Ciencias Exatas", CourseIcon.MATH),
    // Tecnologia -> COMPUTING
    "TEC" to DepartmentInfo("Tecnologia", CourseIcon.COMPUTING),
    "ECEA" to DepartmentInfo("Tecnologia", CourseIcon.COMPUTING),
    "EST" to DepartmentInfo("Tecnologia", CourseIcon.COMPUTING),
    // Letras e Artes -> ARTS
    "DCI" to DepartmentInfo("Letras e Artes", CourseIcon.ARTS),
    "MEL" to DepartmentInfo("Letras e Artes", CourseIcon.ARTS),
    "LIT" to DepartmentInfo("Letras e Artes", CourseIcon.ARTS),
    "PLET" to DepartmentInfo("Letras e Artes", CourseIcon.ARTS),
    "LET" to DepartmentInfo("Letras e Artes", CourseIcon.ARTS),
    "DLA" to DepartmentInfo("Letras e Artes", CourseIcon.ARTS),
    "LIN" to DepartmentInfo("Letras e Artes", CourseIcon.ARTS),
    "EMC" to DepartmentInfo("Letras e Artes", CourseIcon.ARTS),
    // Ciencias Biologicas -> NATURE
    "BIOT" to DepartmentInfo("Ciencias Biologicas", CourseIcon.NATURE),
    "BIO" to DepartmentInfo("Ciencias Biologicas", CourseIcon.NATURE),
    "BOT" to DepartmentInfo("Ciencias Biologicas", CourseIcon.NATURE),
    "ECO" to DepartmentInfo("Ciencias Biologicas", CourseIcon.NATURE),
    "RGV" to DepartmentInfo("Ciencias Biologicas", CourseIcon.NATURE),
    "EBC" to DepartmentInfo("Ciencias Biologicas", CourseIcon.NATURE),
    // Fisica -> MATH
    "FIS" to DepartmentInfo("Fisica", CourseIcon.MATH),
    "AST" to DepartmentInfo("Fisica", CourseIcon.MATH),
    // Saude -> NATURE
    "SAU" to DepartmentInfo("Saude", CourseIcon.NATURE),
    "EGS" to DepartmentInfo("Saude", CourseIcon.NATURE),
    "ENF" to DepartmentInfo("Saude", CourseIcon.NATURE),
    "SCP" to DepartmentInfo("Saude", CourseIcon.NATURE),
    "FAR" to DepartmentInfo("Saude", CourseIcon.NATURE),
    "RUE" to DepartmentInfo("Saude", CourseIcon.NATURE),
    "RSF" to DepartmentInfo("Saude", CourseIcon.NATURE),
    // Educacao -> GENERAL
    "EDU" to DepartmentInfo("Educacao", CourseIcon.GENERAL),
    "PGE" to DepartmentInfo("Educacao", CourseIcon.GENERAL),
    "EEI" to DepartmentInfo("Educacao", CourseIcon.GENERAL),
    "ECD" to DepartmentInfo("Educacao", CourseIcon.GENERAL),
    // Humanas e Filosofia -> GENERAL
    "CHF" to DepartmentInfo("Humanas e Filosofia", CourseIcon.GENERAL),
    "PLA" to DepartmentInfo("Humanas e Filosofia", CourseIcon.GENERAL),
    "PGH" to DepartmentInfo("Humanas e Filosofia", CourseIcon.GENERAL),
    "FIL" to DepartmentInfo("Humanas e Filosofia", CourseIcon.GENERAL),
    "EHB" to DepartmentInfo("Humanas e Filosofia", CourseIcon.GENERAL),
    // Sociais Aplicadas -> GENERAL
    "CIS" to DepartmentInfo("Sociais Aplicadas", CourseIcon.GENERAL),
    "EGU" to DepartmentInfo("Sociais Aplicadas", CourseIcon.GENERAL),
    "GP" to DepartmentInfo("Sociais Aplicadas", CourseIcon.GENERAL),
    "EGP" to DepartmentInfo("Sociais Aplicadas", CourseIcon.GENERAL),
    "CGE" to DepartmentInfo("Sociais Aplicadas", CourseIcon.GENERAL),
    "GOS" to DepartmentInfo("Sociais Aplicadas", CourseIcon.GENERAL),
    // Extensao -> GENERAL
    "UCE" to DepartmentInfo("Extensao", CourseIcon.GENERAL),
    // Pos-Graduacao -> GENERAL
    "PGCI" to DepartmentInfo("Pos-Graduacao", CourseIcon.GENERAL),
)

private val fallbackDepartment = DepartmentInfo("Geral", CourseIcon.GENERAL)

private fun extractPrefix(code: String): String {
    return code.takeWhile { it.isLetter() }
}

private fun resolveDepartment(activityCode: String): DepartmentInfo {
    val prefix = extractPrefix(activityCode)
    return departmentsByPrefix[prefix] ?: fallbackDepartment
}

private val dayNames = mapOf(
    1 to "Dom",
    2 to "Seg",
    3 to "Ter",
    4 to "Qua",
    5 to "Qui",
    6 to "Sex",
    7 to "Sab"
)

private fun formatTime(time: String): String {
    // "HH:MM:SS" -> "HH:MM"
    return time.substringBeforeLast(":")
}

private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hours * 60 + minutes
}

private fun EnrollmentOffer.Allocation.toTimeSlot(): TimeSlot {
    return TimeSlot(
        day = schedule.day,
        startMinutes = timeToMinutes(schedule.start),
        endMinutes = timeToMinutes(schedule.end)
    )
}

internal fun formatSchedule(allocations: List<EnrollmentOffer.Allocation>): String {
    if (allocations.isEmpty()) return "Horário não definido"

    return allocations
        .groupBy { formatTime(it.schedule.start) + " - " + formatTime(it.schedule.end) }
        .map { (timeRange, allocs) ->
            val days = allocs
                .mapNotNull { dayNames[it.schedule.day] }
                .distinct()
                .joinToString("/")
            "$days $timeRange"
        }
        .joinToString(" | ")
}

internal fun EnrollmentOffer.toCatalogCourseItem(): CatalogCourseItem {
    val dept = resolveDepartment(activity.code)
    val preSelectedIndex = classes.indexOfFirst { it.proposalSaved || it.enrollmentConfirmed }
    val isPreSelected = preSelectedIndex >= 0
    return CatalogCourseItem(
        id = id.toString(),
        code = activity.code,
        name = activity.name,
        department = dept.name,
        type = if (mandatory) CourseType.MANDATORY else CourseType.ELECTIVE,
        icon = dept.icon,
        creditsHours = activity.workload,
        expanded = false,
        selected = isPreSelected,
        hasConflict = false,
        selectedGroupIndex = if (isPreSelected) preSelectedIndex else 0,
        groups = classes.map { it.toCourseGroupDetails() }
    )
}

internal fun EnrollmentOffer.ClassOfferItem.toCourseGroupDetails(): CourseGroupDetails {
    val allAllocations = classGroup.classes.flatMap { it.allocations }
    val professor = classGroup.classes
        .firstOrNull()
        ?.professors
        ?.firstOrNull()
        ?.displayName
        ?: "Não definido"
    val enrolledCount = classGroup.classes.sumOf { it.studentCount }

    return CourseGroupDetails(
        groupName = classGroup.description,
        schedule = formatSchedule(allAllocations),
        professor = professor,
        enrolledCount = enrolledCount,
        totalVacancies = vacancies,
        allocations = allAllocations.map { it.toTimeSlot() }
    )
}

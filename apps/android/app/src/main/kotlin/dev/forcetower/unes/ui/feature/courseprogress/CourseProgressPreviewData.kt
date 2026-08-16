package dev.forcetower.unes.ui.feature.courseprogress

import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntry
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumEntryStatus
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumPeriod
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumRequirementKind
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumRequirementProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumSummary
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumVersion
import kotlin.time.Instant

// Preview-only fixture: three períodos of the dc Psicologia scenario, enough
// to exercise every situation, a stale grid, and an unmeasurable bucket. The
// live screens never touch this.
internal object CourseProgressPreviewData {
    private fun entry(
        code: String,
        name: String,
        hours: Int,
        period: Int,
        status: CurriculumEntryStatus,
        prerequisites: List<String> = emptyList(),
        corequisites: List<String> = emptyList(),
        requirementCode: String = "nc",
    ) = CurriculumEntry(
        code = code,
        name = name,
        hours = hours,
        credits = null,
        period = period,
        coreqGroup = null,
        requirementCode = requirementCode,
        status = status,
        prerequisites = prerequisites,
        corequisites = corequisites,
    )

    val progress = CourseProgress(
        curriculum = CurriculumVersion(
            id = "cur-1",
            code = "20232",
            label = "BACHAREL E FORMAÇÃO DE PSICÓLOGO",
            asOf = "2024-03-04",
            minPeriods = 10,
            maxPeriods = 16,
            stale = true,
        ),
        summary = CurriculumSummary(
            completedHours = 1500,
            requiredHours = 4040,
            percent = 37.1,
            excludedHours = 200,
            unclassifiedHours = 0,
            disciplinesCompleted = 13,
            disciplinesTotal = 21,
        ),
        requirements = listOf(
            CurriculumRequirementProgress(
                code = "nc",
                kind = CurriculumRequirementKind.Required,
                label = "Núcleo Comum + Ênfase",
                shortLabel = "Núcleo Comum + Ênfase",
                startsAtPeriod = 1,
                hoursRequired = 2820,
                hoursCompleted = 1500,
                derivable = true,
                percent = 53.19,
            ),
            CurriculumRequirementProgress(
                code = "eb",
                kind = CurriculumRequirementKind.Internship,
                label = "Estágio Básico",
                shortLabel = "Estágio Básico",
                startsAtPeriod = 4,
                hoursRequired = 360,
                hoursCompleted = 0,
                derivable = true,
                percent = 0.0,
            ),
            CurriculumRequirementProgress(
                code = "ac",
                kind = CurriculumRequirementKind.Complementary,
                label = "Atividade Complementar",
                shortLabel = "Atividade Complementar",
                startsAtPeriod = null,
                hoursRequired = 200,
                hoursCompleted = 0,
                derivable = false,
                percent = 0.0,
            ),
        ),
        periods = listOf(
            CurriculumPeriod(
                period = 1,
                entries = listOf(
                    entry("CHF289", "HIST. E EPISTEM. DA PSICOLOGIA", 60, 1, CurriculumEntryStatus.Completed),
                    entry("CHF288", "PROCESSOS PSICOLÓGI. BÁSICOS 1", 60, 1, CurriculumEntryStatus.Completed),
                    entry("SAU281", "ANATOMIA E FISIOLOGIA HUMANA", 60, 1, CurriculumEntryStatus.Completed),
                ),
            ),
            CurriculumPeriod(
                period = 2,
                entries = listOf(
                    entry(
                        "CHF299", "PROCESSOS PSICOLÓGI. BÁSICOS 2", 60, 2,
                        CurriculumEntryStatus.Completed, prerequisites = listOf("CHF288"),
                    ),
                    entry("CHF292", "MÉTOD. E TÉCN. DE PESQ. EM PSI", 60, 2, CurriculumEntryStatus.Failed),
                ),
            ),
            CurriculumPeriod(
                period = 3,
                entries = listOf(
                    entry(
                        "CHF344", "AVALIAÇÃO PSICOLÓGICA 1", 60, 3,
                        CurriculumEntryStatus.InProgress,
                        prerequisites = listOf("CHF299"),
                        corequisites = listOf("CHF302"),
                    ),
                    entry(
                        "CHF302", "TÉCN. DE ENTREVISTA EM PSIC.", 45, 3,
                        CurriculumEntryStatus.InProgress, prerequisites = listOf("CHF299"),
                    ),
                    entry("CHF303", "PROCESSOS GRUPAIS", 45, 3, CurriculumEntryStatus.Withdrawn),
                    entry("CHF304", "PSICOLOGIA E SAÚDE COLETIVA", 45, 3, CurriculumEntryStatus.Available),
                ),
            ),
            CurriculumPeriod(
                period = 4,
                entries = listOf(
                    entry(
                        "CHF345", "AVALIAÇÃO PSICOLÓGICA 2", 60, 4,
                        CurriculumEntryStatus.Blocked, prerequisites = listOf("CHF344"),
                    ),
                    entry(
                        "CHF353", "ESTÁGIO BÁSICO 1", 120, 4,
                        CurriculumEntryStatus.Blocked,
                        prerequisites = listOf("CHF302"),
                        requirementCode = "eb",
                    ),
                    entry("CHF311", "PSICOPATOLOGIA GERAL", 60, 4, CurriculumEntryStatus.NotTaken),
                ),
            ),
        ),
        currentPeriod = 3,
        prerequisitesKnown = true,
        syncedAt = Instant.fromEpochMilliseconds(1_786_000_000_000),
    )
}

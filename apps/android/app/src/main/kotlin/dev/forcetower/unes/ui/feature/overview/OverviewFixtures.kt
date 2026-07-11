package dev.forcetower.unes.ui.feature.overview

// UI projection types for the "Hoje" screen (2026 redesign — dc project
// `UNES Home - Android`). The screen maps raw KMP payloads into these; the
// same types feed Compose previews on the components via `OverviewFixtures`.

// Class payload shared by the hero's upcoming/live states.
internal data class OverviewHeroClass(
    // Null only for fixture/pre-sync data — the CTA renders non-tappable.
    val offerId: String?,
    val code: String,
    val title: String,
    val prof: String?,
    val room: String?,
    val timeRange: String,
)

// The three hero states from the design: 3a (live), 3b (day done) and the
// default "Próxima aula" card.
internal sealed interface OverviewHeroState {
    data class Upcoming(
        val klass: OverviewHeroClass,
        val startsInMinutes: Int,
    ) : OverviewHeroState

    data class Live(
        val klass: OverviewHeroClass,
        val endsInMinutes: Int,
        // 0f..1f — elapsed fraction of the class window.
        val progress: Float,
    ) : OverviewHeroState

    data class DayDone(
        val classCount: Int,
        // Null when tomorrow has no classes — the "Amanhã" block is hidden.
        val tomorrow: OverviewTomorrowUi?,
    ) : OverviewHeroState
}

internal data class OverviewTomorrowUi(
    val title: String,
    // "08:00" — the time chip splits it into hour + minute lines.
    val startTime: String,
    val room: String?,
    val extraCount: Int,
)

// "Reta final" card — the exam variant wins whenever an evaluation is
// scheduled; otherwise the card counts down to the semester end.
internal sealed interface OverviewFinalStretch {
    data class Exam(
        val label: String,
        val disciplineName: String,
        val daysUntil: Int,
        val dateLabel: String,
    ) : OverviewFinalStretch

    data class Semester(
        val daysLeft: Int,
        val semesterLabel: String,
    ) : OverviewFinalStretch
}

internal enum class OverviewClassState { Done, Now, Next, Later }

internal data class OverviewTodayItem(
    val offerId: String?,
    val code: String,
    val title: String,
    val startTime: String,
    val endTime: String?,
    val room: String?,
    val state: OverviewClassState,
)

internal data class OverviewMessagePreview(
    val unreadCount: Int,
    val sender: String,
    val preview: String,
    // Relative label ("2h") — null hides the chip.
    val timeLabel: String?,
)

// Minimal seed handed back to the Connected shell when a class row/CTA is
// tapped; the detail screen hydrates the full payload from `offerId`.
internal data class OverviewDiscipline(
    val code: String,
    val title: String,
    val offerId: String?,
)

internal object OverviewFixtures {
    const val DATE_EYEBROW = "Ter, 11 mar"
    const val TOMORROW_EYEBROW = "Qua, 12 mar"
    const val WEEKDAY = "Terça"
    const val COURSE_LINE = "Ciência da Computação · 6º período"

    val heroClass = OverviewHeroClass(
        offerId = null,
        code = "MT304",
        title = "Cálculo III",
        prof = "Dra. Helena Braga",
        room = "Bloco PA · sala 204",
        timeRange = "19:00 – 20:40",
    )

    val heroUpcoming = OverviewHeroState.Upcoming(klass = heroClass, startsInMinutes = 120)

    val heroLive = OverviewHeroState.Live(klass = heroClass, endsInMinutes = 38, progress = 0.63f)

    val heroDayDone = OverviewHeroState.DayDone(
        classCount = 4,
        tomorrow = OverviewTomorrowUi(
            title = "Sistemas Operacionais",
            startTime = "08:00",
            room = "Bloco PA · sala 112",
            extraCount = 2,
        ),
    )

    val finalStretchSemester = OverviewFinalStretch.Semester(daysLeft = 23, semesterLabel = "2025.1")

    val finalStretchExam = OverviewFinalStretch.Exam(
        label = "P2",
        disciplineName = "Cálculo III",
        daysUntil = 10,
        dateLabel = "Seg, 21 mar",
    )

    val today = listOf(
        OverviewTodayItem(
            offerId = null, code = "EDII", title = "Estrutura de Dados II",
            startTime = "10:00", endTime = "11:40", room = "Lab. de Informática 3",
            state = OverviewClassState.Done,
        ),
        OverviewTodayItem(
            offerId = null, code = "BD", title = "Banco de Dados",
            startTime = "14:00", endTime = "15:40", room = "Bloco PA · sala 108",
            state = OverviewClassState.Done,
        ),
        OverviewTodayItem(
            offerId = null, code = "MT304", title = "Cálculo III",
            startTime = "19:00", endTime = "20:40", room = "Bloco PA · sala 204",
            state = OverviewClassState.Next,
        ),
        OverviewTodayItem(
            offerId = null, code = "ESII", title = "Engenharia de Software II",
            startTime = "21:00", endTime = "22:40", room = "Bloco PA · sala 210",
            state = OverviewClassState.Later,
        ),
    )

    val messagePreview = OverviewMessagePreview(
        unreadCount = 3,
        sender = "Coordenação do curso",
        preview = "Prazo de rematrícula até sexta-feira",
        timeLabel = "2h",
    )
}

package dev.forcetower.unes.ui.feature.disciplines

// Preview/sample data for the Disciplinas screen — mirrors the mock payload of
// the dc prototype (`UNES Disciplinas - Android.dc.html`) so previews render
// the same composition the design was approved on. Colors are left
// `Unspecified`; previews resolve them via `tinted(MaterialTheme.melon.palette)`.
internal object DisciplinesFixtures {

    val CURRENT = Semester(
        id = "2025.2",
        disciplines = listOf(
            discipline(
                code = "EXA805",
                title = "Algoritmos e Programação II",
                prof = "João Batista da Rocha Junior",
                scores = listOf(8.3, 10.0, 8.0),
                absences = 6,
            ),
            discipline(
                code = "EXA704",
                title = "Cálculo Diferencial e Integral I E",
                prof = "Rogério Gomes Matias",
                scores = listOf(2.0, 0.0, 8.5),
                absences = 0,
                finalGrade = 3.5,
            ),
            discipline(
                code = "EXA807",
                title = "Estruturas Discretas",
                prof = "Thiago Pires Santana",
                scores = listOf(8.0, 9.2, 8.5),
                absences = 3,
            ),
            discipline(
                code = "EXA863",
                title = "MI · Programação",
                prof = "Matheus Giovanni Pereira",
                scores = listOf(5.8, 5.7, null),
                absences = 8,
            ),
        ),
        dbSemesterId = "sem-2025-2",
    )

    val PAST = listOf(
        Semester(
            id = "2025.1",
            disciplines = listOf(
                pastDiscipline("EXA415", "Algoritmos e Programação I", 7.5, approved = true),
                pastDiscipline("MAT202", "Cálculo Diferencial e Integral II", 6.8, approved = true),
                pastDiscipline("EXA512", "Arquitetura de Computadores", 7.1, approved = true),
                pastDiscipline("EXA608", "Sistemas Digitais", 8.0, approved = true),
                pastDiscipline("HUM101", "Metodologia Científica", 7.1, approved = true),
            ),
            dbSemesterId = "sem-2025-1",
        ),
        Semester(
            id = "2024.2",
            disciplines = listOf(
                pastDiscipline("MAT101", "Cálculo Diferencial e Integral I", 5.4, approved = true),
                pastDiscipline("EXA411", "Introdução à Programação", 8.2, approved = true),
                pastDiscipline("FIS201", "Física I", 4.1, approved = false),
                pastDiscipline("QUI101", "Química Geral", 6.9, approved = true),
                pastDiscipline("EXA108", "Lógica para Computação", 7.7, approved = true),
                pastDiscipline("HUM204", "Filosofia da Ciência", 3.8, approved = false),
                pastDiscipline("LET101", "Português Instrumental", 7.0, approved = true),
            ),
            dbSemesterId = "sem-2024-2",
        ),
    )

    val PENDING = listOf(
        Semester(id = "2024.1", isDownloaded = false, dbSemesterId = "sem-2024-1"),
    )

    const val OVERALL_SCORE = 7.0

    private fun discipline(
        code: String,
        title: String,
        prof: String,
        scores: List<Double?>,
        absences: Int,
        finalGrade: Double? = null,
    ): Discipline {
        val grades = scores.mapIndexed { index, score ->
            GradeEntry(label = "AV${index + 1}", title = "Avaliação ${index + 1}", date = null, score = score)
        }
        val released = grades.mapNotNull { it.score }
        return Discipline(
            code = code,
            fullCode = code,
            title = title,
            dept = "Ciências Exatas",
            prof = prof,
            color = androidx.compose.ui.graphics.Color.Unspecified,
            hours = 60,
            absences = absences,
            allowedAbsences = 15,
            sections = listOf(GradeSection(name = "Geral", grades = grades)),
            finalGrade = finalGrade,
            storedPartialAverage = if (released.isEmpty()) null else released.sum() / released.size,
            offerId = "offer-$code",
        )
    }

    private fun pastDiscipline(
        code: String,
        title: String,
        finalGrade: Double,
        approved: Boolean,
    ): Discipline = Discipline(
        code = code,
        fullCode = code,
        title = title,
        dept = "Ciências Exatas",
        prof = "",
        color = androidx.compose.ui.graphics.Color.Unspecified,
        hours = 60,
        absences = 0,
        allowedAbsences = 15,
        sections = emptyList(),
        finalGrade = finalGrade,
        approved = approved,
        offerId = "offer-$code",
    )
}

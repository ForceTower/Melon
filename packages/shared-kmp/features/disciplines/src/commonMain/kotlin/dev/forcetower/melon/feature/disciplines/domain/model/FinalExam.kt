package dev.forcetower.melon.feature.disciplines.domain.model

// SAGRES's final exam row: named "Prova Final" with the "Adicional" short
// label. Both must match — teachers also give regular evaluations that name,
// but those carry AV-style short labels. The exam closes the discipline on its
// own (0.6 × mean + 0.4 × exam), so it never joins the partial mean.
fun isFinalExam(
    name: String,
    nameShort: String?,
): Boolean =
    name.trim().equals("prova final", ignoreCase = true) &&
        nameShort?.trim().equals("adicional", ignoreCase = true) == true

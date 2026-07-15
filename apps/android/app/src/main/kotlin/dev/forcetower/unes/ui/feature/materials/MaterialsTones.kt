package dev.forcetower.unes.ui.feature.materials

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.forcetower.melon.feature.materials.domain.model.MaterialType
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Fixed type → tint/icon/label mapping from the dc `MateriaisScreen` TYPES
// table: prova=coral, lista=teal, resumo=magenta, formulário=amber. The
// palette slots already carry the light/dark lift the design encodes by hand.
@Composable
@ReadOnlyComposable
internal fun MaterialType.hue(): Color {
    val palette = MaterialTheme.melon.palette
    return when (this) {
        MaterialType.Exam -> palette.coral
        MaterialType.SolvedList -> palette.teal
        MaterialType.Summary -> palette.magenta
        MaterialType.FormulaSheet -> palette.amber
    }
}

internal fun MaterialType.icon(): ImageVector = when (this) {
    MaterialType.Exam -> Icons.Filled.Quiz
    MaterialType.SolvedList -> Icons.Filled.Checklist
    MaterialType.Summary -> Icons.AutoMirrored.Filled.StickyNote2
    MaterialType.FormulaSheet -> Icons.Filled.Functions
}

internal fun MaterialType.labelRes(): Int = when (this) {
    MaterialType.Exam -> R.string.materials_type_exam
    MaterialType.SolvedList -> R.string.materials_type_list
    MaterialType.Summary -> R.string.materials_type_summary
    MaterialType.FormulaSheet -> R.string.materials_type_formula
}

internal fun MaterialType.pluralLabelRes(): Int = when (this) {
    MaterialType.Exam -> R.string.materials_type_exam_plural
    MaterialType.SolvedList -> R.string.materials_type_list_plural
    MaterialType.Summary -> R.string.materials_type_summary_plural
    MaterialType.FormulaSheet -> R.string.materials_type_formula_plural
}

// "3 provas" / "1 lista" — the lowercase tally chips on the hub cards.
internal fun MaterialType.tallyRes(): Int = when (this) {
    MaterialType.Exam -> R.plurals.materials_tally_exam
    MaterialType.SolvedList -> R.plurals.materials_tally_list
    MaterialType.Summary -> R.plurals.materials_tally_summary
    MaterialType.FormulaSheet -> R.plurals.materials_tally_formula
}

// Stable presentation order everywhere types are enumerated (tallies, filter
// chips, the upload type grid) — mirrors the dc ORDER array.
internal val MaterialTypeOrder = listOf(
    MaterialType.Exam,
    MaterialType.SolvedList,
    MaterialType.Summary,
    MaterialType.FormulaSheet,
)

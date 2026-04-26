package dev.forcetower.unes.ui.feature.overview

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.theme.MelonPaletteColors
import dev.forcetower.unes.designsystem.theme.melon
import kotlin.math.absoluteValue

// Maps a discipline code to a stable visual color and mesh variant so the same
// code lands on the same tint across NowCard, TodayTimeline, and the
// DisciplinesStrip. Mirrors iOS `ColorFor` in `OverviewViewModel.swift` —
// djb2 with seed 5381 over Char codes, byte-identical so a code resolves to
// the same bucket on both platforms.
internal object ColorFor {
    @Composable
    @ReadOnlyComposable
    fun discipline(code: String): Color = discipline(MaterialTheme.melon.palette, code)

    fun discipline(palette: MelonPaletteColors, code: String): Color {
        val slots = listOf(
            palette.coral, palette.amber, palette.magenta, palette.teal, palette.plum,
            palette.rose, palette.sky, palette.emerald, palette.indigo, palette.mustard,
        )
        return slots[stableHash(code).absoluteValue % slots.size]
    }

    fun meshVariant(code: String): MeshVariant {
        val variants = listOf(MeshVariant.Cool, MeshVariant.Warm, MeshVariant.Rose)
        return variants[stableHash(code).absoluteValue % variants.size]
    }

    private fun stableHash(s: String): Int {
        var h = 5381
        for (c in s) h = ((h shl 5) + h) + c.code
        return h
    }
}

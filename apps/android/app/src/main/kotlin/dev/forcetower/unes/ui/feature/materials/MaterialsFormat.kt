package dev.forcetower.unes.ui.feature.materials

import android.icu.text.CompactDecimalFormat
import java.util.Locale

internal object MaterialsFormat {
    // Locale-aware "1,2 mil" style compact count for the download tally.
    fun compactCount(value: Int): String {
        if (value < 1000) return value.toString()
        return CompactDecimalFormat.getInstance(
            Locale.getDefault(),
            CompactDecimalFormat.CompactStyle.SHORT,
        ).format(value)
    }
}

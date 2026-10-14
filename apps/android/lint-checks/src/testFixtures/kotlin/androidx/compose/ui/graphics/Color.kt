package androidx.compose.ui.graphics

@JvmInline
value class Color(
    val value: ULong,
) {
    companion object {
        val White = Color(0xFFFFFFFFUL)
        val Transparent = Color(0UL)
        val Unspecified = Color(1UL)
    }
}

fun Color(value: Long): Color = Color(value.toULong())

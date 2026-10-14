package dev.forcetower.unes.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin

internal val colorStub = kotlin(
    """
    package androidx.compose.ui.graphics
    class Color {
        companion object {
            val Red: Color get() = TODO()
            val White: Color get() = TODO()
            val Transparent: Color get() = TODO()
            val Unspecified: Color get() = TODO()
        }
    }
    fun Color(value: Long): Color = TODO()
    fun Color(value: Int): Color = TODO()
    """,
).indented().to("src/androidx/compose/ui/graphics/Color.kt")

internal val fontStub = kotlin(
    """
    package androidx.compose.ui.text.font
    class FontFamily {
        companion object {
            val Default: FontFamily get() = TODO()
            val Monospace: FontFamily get() = TODO()
        }
    }
    """,
).indented()

internal val springStub = kotlin(
    """
    package androidx.compose.animation.core
    fun spring(stiffness: Float = 100f): Any = TODO()
    """,
).indented()

internal val textStub = kotlin(
    """
    package androidx.compose.material3
    fun Text(text: String, modifier: Any? = null) {}
    fun Icon(imageVector: Any, contentDescription: String?, modifier: Any? = null) {}
    """,
).indented()

internal val resourceStub = kotlin(
    """
    package androidx.compose.ui.res
    fun stringResource(id: Int): String = TODO()
    """,
).indented()

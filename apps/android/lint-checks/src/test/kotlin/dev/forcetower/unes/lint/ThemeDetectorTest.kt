package dev.forcetower.unes.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

class ThemeDetectorTest : LintDetectorTest() {
    override fun getDetector() = ThemeDetector()
    override fun getIssues() = listOf(ThemeDetector.COLOR, ThemeDetector.TYPOGRAPHY, ThemeDetector.MOTION)

    fun testCompiledCompanionProperties() {
        lint().files(
            bytes(
                "libs/compose.jar",
                javaClass.getResourceAsStream("/compose-fixtures.jar")!!.use { it.readBytes() },
            ),
            kotlin(
                """
                package example
                import androidx.compose.ui.graphics.Color as Paint
                import androidx.compose.ui.text.font.FontFamily as Typeface
                internal val color = Paint.White
                internal val font = Typeface.Default
                internal val raw = Paint(0xFF123456)
                internal val clear = Paint.Transparent
                internal val inherited = Paint.Unspecified
                """,
            ).indented().to("src/example/CompiledProbe.kt"),
        ).run().expectErrorCount(3)
    }

    fun testAliasedColorConstantsAndPresets() {
        lint().files(
            colorStub,
            kotlin(
                """
                package example
                import androidx.compose.ui.graphics.Color as Paint
                import androidx.compose.ui.graphics.Color.Companion.Red as Stop
                private const val INK = 0xFF123456
                internal fun colors() {
                    val raw = Paint(INK)
                    val preset = Paint.White
                    val alias = Stop
                }
                """,
            ).indented(),
        ).run().expectErrorCount(3).expectContains("[MelonThemeColor]")
    }

    fun testTransparentUnspecifiedAndDynamicWidgetColors() {
        lint().files(
            colorStub,
            kotlin(
                """
                package example
                import androidx.compose.ui.graphics.Color
                internal fun colors(widgetArgb: Int) {
                    val clear = Color.Transparent
                    val inherited = Color.Unspecified
                    val restored = Color(widgetArgb)
                }
                """,
            ).indented(),
        ).run().expectClean()
    }

    fun testFontAliasesAndConstructors() {
        lint().files(
            fontStub,
            kotlin(
                """
                package example
                import androidx.compose.ui.text.font.FontFamily as Typeface
                import androidx.compose.ui.text.font.FontFamily.Companion.Monospace as CodeFont
                internal val default = Typeface.Default
                internal val mono = CodeFont
                internal val custom = Typeface()
                """,
            ).indented(),
        ).run().expectErrorCount(3).expectContains("[MelonThemeTypography]")
    }

    fun testQualifiedAndAliasedSprings() {
        lint().files(
            springStub,
            kotlin(
                """
                package example
                import androidx.compose.animation.core.spring as bounce
                internal val first = bounce(stiffness = 900f)
                internal val second = androidx.compose.animation.core.spring()
                """,
            ).indented(),
        ).run().expectErrorCount(2).expectContains("[MelonThemeMotion]")
    }

    fun testBackingTokensResolveThroughAliases() {
        lint().files(
            colorStub,
            fontStub,
            kotlin(
                """
                package dev.forcetower.unes.designsystem.theme
                import androidx.compose.ui.graphics.Color
                internal val BrandPlum: Color get() = TODO()
                """,
            ).indented().to("src/dev/forcetower/unes/designsystem/theme/Color.kt"),
            kotlin(
                """
                package dev.forcetower.unes.designsystem.theme
                import androidx.compose.ui.text.font.FontFamily
                internal val MelonSans: FontFamily get() = TODO()
                """,
            ).indented().to("src/dev/forcetower/unes/designsystem/theme/Fonts.kt"),
            kotlin(
                """
                package example
                import dev.forcetower.unes.designsystem.theme.BrandPlum as Ink
                import dev.forcetower.unes.designsystem.theme.MelonSans as Sans
                internal val color = Ink
                internal val font = Sans
                """,
            ).indented(),
        ).run().expectErrorCount(2)
            .expectContains("[MelonThemeColor]").expectContains("[MelonThemeTypography]")
    }

    fun testUnrelatedSymbolsAndThemedValues() {
        lint().files(
            colorStub,
            fontStub,
            kotlin(
                """
                package example
                import androidx.compose.ui.graphics.Color as ComposeColor
                import androidx.compose.ui.text.font.FontFamily as ComposeFont
                internal object MaterialTheme {
                    val color: ComposeColor get() = TODO()
                    val font: ComposeFont get() = TODO()
                }
                internal object MelonMotion { fun spring(): Any = TODO() }
                internal fun Color(value: Int) = value
                internal object FontFamily { val Default = 1 }
                internal val BrandPlum = 2
                internal val values = listOf(
                    MaterialTheme.color, MaterialTheme.font, MelonMotion.spring(),
                    Color(123), FontFamily.Default, BrandPlum
                )
                """,
            ).indented(),
        ).run().expectClean()
    }

    fun testTestSourcesAreExcluded() {
        lint().files(
            colorStub,
            kotlin(
                """
                package example
                import androidx.compose.ui.graphics.Color
                class Fixture { val color = Color.Red }
                """,
            ).indented().within("test"),
        ).run().expectClean()
    }
}

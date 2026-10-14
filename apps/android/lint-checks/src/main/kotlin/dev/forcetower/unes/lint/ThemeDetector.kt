package dev.forcetower.unes.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParentOfType

class ThemeDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf("Color", "FontFamily", "spring")

    override fun getApplicableConstructorTypes() = listOf(COLOR_CLASS, FONT_CLASS)

    override fun visitConstructor(
        context: JavaContext,
        node: UCallExpression,
        constructor: PsiMethod,
    ) {
        inspectCall(context, node, constructor)
    }

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod,
    ) {
        inspectCall(context, node, method)
    }

    private fun inspectCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod,
    ) {
        if (context.isTestSource || context.isGeneratedSource) return
        val owner = method.containingClass?.qualifiedName ?: return
        val issue = when {
            (owner == COLOR_CLASS || owner == "${COLOR_CLASS}Kt") &&
                node.valueArguments.any { ConstantEvaluator.evaluate(context, it) is Number } -> COLOR
            owner == FONT_CLASS || owner == "${FONT_CLASS}Kt" -> TYPOGRAPHY
            method.name == "spring" && owner.startsWith("androidx.compose.animation.core.") -> MOTION
            else -> return
        }
        context.report(issue, node, context.getLocation(node), message(issue))
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(USimpleNameReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        if (context.isTestSource || context.isGeneratedSource) return null
        return object : UElementHandler() {
            override fun visitSimpleNameReferenceExpression(node: USimpleNameReferenceExpression) {
                if (node.getParentOfType(UImportStatement::class.java) != null) return
                val member = node.resolve() as? PsiMember ?: return
                val owner = member.containingClass?.qualifiedName ?: return
                val name = member.name?.removePrefix("get")?.substringBefore('-')?.substringBefore('$')
                // Compiled companion properties can resolve to fields on the outer class.
                val issue = when {
                    (owner == COLOR_CLASS || owner == "$COLOR_CLASS.Companion") && name in PRESET_COLORS -> COLOR
                    (owner == FONT_CLASS || owner == "$FONT_CLASS.Companion") && name in PRESET_FONTS -> TYPOGRAPHY
                    owner == "${THEME_PACKAGE}.ColorKt" -> COLOR
                    owner == "${THEME_PACKAGE}.FontsKt" -> TYPOGRAPHY
                    else -> return
                }
                context.report(issue, node, context.getLocation(node), message(issue))
            }
        }
    }

    private fun message(issue: Issue) =
        when (issue) {
            COLOR -> "Use a theme color; define new colors in the design system first"
            TYPOGRAPHY -> "Use MaterialTheme.typography or MelonWidgetFonts instead of a raw font family"
            else -> "Use MelonMotion instead of a raw Compose spring"
        }

    companion object {
        private const val COLOR_CLASS = "androidx.compose.ui.graphics.Color"
        private const val FONT_CLASS = "androidx.compose.ui.text.font.FontFamily"
        private const val THEME_PACKAGE = "dev.forcetower.unes.designsystem.theme"
        private val PRESET_COLORS = setOf(
            "Black", "White", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "Gray", "LightGray", "DarkGray",
        )
        private val PRESET_FONTS = setOf("Default", "SansSerif", "Serif", "Monospace", "Cursive")
        private val IMPLEMENTATION = Implementation(ThemeDetector::class.java, Scope.JAVA_FILE_SCOPE)

        val COLOR = Issue.create(
            id = "MelonThemeColor",
            briefDescription = "Color bypasses the theme",
            explanation = "Use adaptive or brand theme colors. Transparent and Unspecified are allowed, " +
                "as are runtime colors restored from themed widget data. Constant colors belong in the design system.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION,
        )
        val TYPOGRAPHY = Issue.create(
            id = "MelonThemeTypography",
            briefDescription = "Font family bypasses the theme",
            explanation = "Use MaterialTheme.typography, its monoFontFamily extension, or MelonWidgetFonts. " +
                "Raw font families and backing font definitions belong in the design system.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION,
        )
        val MOTION = Issue.create(
            id = "MelonThemeMotion",
            briefDescription = "Spring bypasses motion tokens",
            explanation = "Use MelonMotion so spring behavior stays consistent across the application.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION,
        )
    }
}

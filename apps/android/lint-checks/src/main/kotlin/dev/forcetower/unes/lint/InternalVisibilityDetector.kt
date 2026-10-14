package dev.forcetower.unes.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile

class InternalVisibilityDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UFile::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler? {
        if (context.isTestSource || context.isGeneratedSource) return null
        return object : UElementHandler() {
            override fun visitFile(node: UFile) {
                val file = node.sourcePsi as? KtFile ?: return
                for (declaration in file.declarations) {
                    if (declaration.hasModifier(KtTokens.INTERNAL_KEYWORD) ||
                        declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)
                    ) {
                        continue
                    }
                    context.report(
                        ISSUE,
                        declaration,
                        context.getNameLocation(declaration),
                        "Declare app-level symbols internal or private",
                    )
                }
            }
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "MelonInternalVisibility",
            briefDescription = "App declaration exposes a public API",
            explanation = "Top-level Kotlin declarations in the app must be internal or private. " +
                "Cross-module APIs belong in library modules; nested members retain their normal visibility.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(InternalVisibilityDetector::class.java, Scope.JAVA_FILE_SCOPE),
        )
    }
}

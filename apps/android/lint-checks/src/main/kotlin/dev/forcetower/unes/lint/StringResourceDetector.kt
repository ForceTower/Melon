package dev.forcetower.unes.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.ConstantEvaluator
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

class StringResourceDetector :
    Detector(),
    SourceCodeScanner {
    override fun getApplicableMethodNames() = listOf("Text", "BasicText", "Icon", "Image")

    override fun visitMethodCall(
        context: JavaContext,
        node: UCallExpression,
        method: PsiMethod,
    ) {
        if (context.isTestSource || context.isGeneratedSource) return
        val owner = method.containingClass?.qualifiedName ?: return
        if (!owner.startsWith("androidx.compose.") && !owner.startsWith("androidx.glance.")) return
        for ((argument, parameter) in context.evaluator.computeArgumentMapping(node, method)) {
            if (parameter.name != "text" && parameter.name != "contentDescription") continue
            val value = ConstantEvaluator.evaluate(context, argument)
            if (value !is String) continue
            context.report(
                ISSUE,
                argument,
                context.getLocation(argument),
                "Move user-facing text to strings.xml and read it through stringResource or getString",
            )
        }
    }

    companion object {
        val ISSUE = Issue.create(
            id = "MelonStringResource",
            briefDescription = "User-facing text is not a string resource",
            explanation = "Compose and Glance text and accessibility descriptions must use string resources. " +
                "This includes literals passed through constants, aliases and constant expressions. " +
                "Runtime content and null accessibility descriptions are allowed.",
            category = Category.I18N,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(StringResourceDetector::class.java, Scope.JAVA_FILE_SCOPE),
        )
    }
}

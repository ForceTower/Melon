package dev.forcetower.unes.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest

class StringResourceDetectorTest : LintDetectorTest() {
    override fun getDetector() = StringResourceDetector()
    override fun getIssues() = listOf(StringResourceDetector.ISSUE)

    fun testTextAliasesConstantsAndExpressions() {
        lint().files(
            textStub,
            kotlin(
                """
                package example
                import androidx.compose.material3.Text as Label
                private const val TITLE = "Title"
                internal fun content() {
                    Label(TITLE)
                    Label(text = "Try " + "again")
                    val description = "Description"
                    Label(modifier = null, text = description)
                }
                """,
            ).indented(),
        ).run().expectErrorCount(3).expectContains("[MelonStringResource]")
    }

    fun testDescriptionConstantsAndReorderedArguments() {
        lint().files(
            textStub,
            kotlin(
                """
                package example
                import androidx.compose.material3.Icon
                private const val BACK = "Back"
                internal fun content(image: Any) {
                    Icon(image, BACK)
                    Icon(contentDescription = "Close", imageVector = image)
                }
                """,
            ).indented(),
        ).run().expectErrorCount(2)
    }

    fun testResourcesRuntimeContentNullAndUnrelatedFunctions() {
        lint().files(
            textStub,
            resourceStub,
            kotlin(
                """
                package example
                import androidx.compose.material3.Icon
                import androidx.compose.material3.Text as Label
                import androidx.compose.ui.res.stringResource
                internal fun Text(text: String) {}
                internal fun content(name: String, image: Any) {
                    Label(stringResource(1))
                    Label(name)
                    Icon(image, null)
                    Icon(image, stringResource(2))
                    Text("A non-UI function")
                }
                """,
            ).indented(),
        ).run().expectClean()
    }
}

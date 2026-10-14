package dev.forcetower.unes.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode

class InternalVisibilityDetectorTest : LintDetectorTest() {
    override fun getDetector() = InternalVisibilityDetector()
    override fun getIssues() = listOf(InternalVisibilityDetector.ISSUE)

    // This mode adds public type aliases, which intentionally violate the rule.
    override fun lint() = super.lint().skipTestModes(TestMode.TYPE_ALIAS)

    fun testPublicDeclarationsWithAnnotationsAndGenerics() {
        lint().files(
            kotlin(
                """
                package example
                internal annotation class Marker(val label: String)
                @Marker("annotation arguments")
                public class Screen<T>
                fun <T> identity(value: T): T = value
                val title = "Fixture"
                typealias Callback = () -> Unit
                fun interface Listener { fun changed() }
                """,
            ).indented(),
        ).run().expectErrorCount(5).expectContains("[MelonInternalVisibility]")
    }

    fun testScopedDeclarationsAndNestedPublicMembers() {
        lint().files(
            kotlin(
                """
                package example
                internal class Screen {
                    public fun render() {}
                    class State
                }
                private val value = 1
                internal typealias Callback = () -> Unit
                internal fun interface Listener { fun changed() }
                """,
            ).indented(),
        ).run().expectClean()
    }

    fun testTestDeclarationsAreExcluded() {
        lint().files(
            kotlin("package example; class ScreenTest").within("test"),
        ).run().expectClean()
    }
}

package dev.forcetower.unes

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import dev.forcetower.unes.remote.FeatureGates
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

internal class ScenarioJourneyTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val scenario = InstrumentationRegistry.getArguments().getString("scenario") ?: "home.populated"

    @Test
    fun runScenario() {
        check(BuildConfig.SCENARIO) { "Journeys require the isolated scenario build" }
        val isEnrollment = scenario.startsWith("enrollment.")
        val isDisciplines = scenario.startsWith("disciplines.") || scenario.startsWith("discipline.")
        val initial = if (isEnrollment || isDisciplines) {
            scenario
        } else {
            when (scenario) {
                "auth.invalid-credentials", "sync.unavailable" -> scenario
                else -> "home.populated"
            }
        }
        selectScenario(initial)
        if (isEnrollment) {
            val app = instrumentation.targetContext.applicationContext as MelonApp
            app.featureFlags.useScenarioGates(FeatureGates(enrollment = true))
        }
        click(R.string.onboarding_welcome_secondary_cta)
        waitFor(R.string.onboarding_login_submit)
        if (scenario == "auth.login") return
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("scenario")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("synthetic-only")
        awaitWindowFocus()
        Espresso.closeSoftKeyboard()
        click(R.string.onboarding_login_submit)
        if (scenario == "auth.invalid-credentials") {
            waitFor(R.string.onboarding_login_error_invalid_credentials)
            capture("failure-state")
            selectScenario("home.populated")
            click(R.string.onboarding_login_submit)
        }
        if (scenario == "sync.unavailable") {
            waitFor(R.string.onboarding_sync_failed)
            capture("failure-state")
            selectScenario("home.populated")
            click(R.string.onboarding_sync_retry)
        }
        click(R.string.onboarding_ready_cta)
        waitForText("Bom dia, Estudante")
        waitForText("Algoritmos de Exemplo")
        if (scenario == "home.offline-with-cache" || scenario == "auth.session-expired") {
            selectScenario(scenario)
            open("unes://home", refresh = true)
            compose.waitUntil(15_000) {
                serverState().getJSONObject("requestCounts").optInt("GET /api/sync/profile") > 0
            }
            if (scenario == "auth.session-expired") waitFor(R.string.session_expired_banner_title)
            waitForText("Algoritmos de Exemplo")
        }
        if (scenario == "messages.empty") {
            open("unes://messages")
            waitFor(R.string.messages_empty_title)
        }
        if (scenario.startsWith("notification.")) runNotification()
        if (isEnrollment) runEnrollment()
        if (isDisciplines) runDisciplines()
        assertEquals(
            "Unmocked requests must fail the journey",
            0,
            serverState().getJSONArray("unexpectedRequests").length(),
        )
    }

    @After
    fun collectEvidence() = capture("result")

    // Switching the server scenario zeroes its request counts, so any inbox
    // request afterwards was caused by the tap.
    private fun runNotification() {
        selectScenario(scenario)
        if (scenario == "notification.message-missing") {
            tapMessageNotification("00000000-0000-4000-8000-000000000404")
            waitFor(R.string.messages_empty_title)
        } else {
            tapMessageNotification("6d5c4b3a-2f1e-4d0c-9b8a-7f6e5d4c3b2a")
            waitForText("Aviso de Exemplo")
            waitForText("Mensagem sintética usada nas jornadas de notificação.")
        }
        assertTrue(serverState().getJSONObject("requestCounts").optInt("GET /api/sync/messages") > 0)
    }

    // The extras a backend message push carries into the tap intent, delivered
    // to the running app through onNewIntent.
    private fun tapMessageNotification(id: String) {
        compose.activity.startActivity(
            Intent(compose.activity, MainActivity::class.java)
                .putExtra("kind", "message")
                .putExtra("messageId", id)
                .putExtra("url", "unes://messages/$id"),
        )
    }

    private fun runEnrollment() {
        open("unes://me")
        scrollClick(compose.activity.getString(R.string.me_shortcut_enrollment_label))
        click(R.string.enrollment_cta_continue)
        waitFor(R.string.enrollment_offers_title)
        if (scenario == "enrollment.submit-retry") {
            scrollClick("Redes de Exemplo")
            scrollToText(compose.activity.getString(R.string.enrollment_prereq_unmet_title))
            scrollClick(compose.activity.getString(R.string.enrollment_section_queue))
            waitFor(R.string.enrollment_section_selected)
            awaitWindowFocus()
            Espresso.pressBack()
            waitFor(R.string.enrollment_offers_title)
            click(R.string.enrollment_dock_review)
            compose.onAllNodesWithContentDescription(compose.activity.getString(R.string.enrollment_remove))
                .onFirst().performScrollTo().performClick()
            compose.onNodeWithText("Algoritmos de Exemplo").assertDoesNotExist()
            scrollToText(compose.activity.getString(R.string.enrollment_review_prereq_title))
            compose.onNodeWithText(compose.activity.getString(R.string.enrollment_dock_submit)).assertIsEnabled()
            click(R.string.enrollment_dock_submit)
            waitFor(R.string.enrollment_submit_error_title)
            capture("submit-failure")
            assertTrue(serverState().isNull("submitted"))
            assertEquals(1, serverState().getJSONArray("submissionAttempts").length())
            click(R.string.enrollment_ok)
            click(R.string.enrollment_dock_submit)
            waitFor(R.string.enrollment_success_title)
            val state = serverState()
            val submitted = state.getJSONArray("submitted")
            assertEquals(1, submitted.length())
            assertEquals(2031, submitted.getJSONObject(0).getInt("sectionId"))
            assertEquals(false, submitted.getJSONObject(0).getBoolean("allowsOther"))
            assertEquals(true, submitted.getJSONObject(0).getBoolean("waitlist"))
            val attempts = state.getJSONArray("submissionAttempts")
            assertEquals(2, attempts.length())
            assertEquals(attempts.getJSONArray(0).toString(), attempts.getJSONArray(1).toString())
        } else {
            click(R.string.enrollment_dock_review)
            val explanation = when (scenario) {
                "enrollment.schedule-conflict" -> R.string.enrollment_review_conflicts_title
                "enrollment.under-minimum" -> R.string.enrollment_review_under_title
                "enrollment.over-maximum" -> R.string.enrollment_review_over_title
                "enrollment.deadline-expired" -> R.string.enrollment_blocker_deadline
                else -> error("Unsupported enrollment scenario: $scenario")
            }
            if (scenario == "enrollment.deadline-expired") {
                waitFor(explanation)
            } else {
                scrollToText(compose.activity.getString(explanation))
            }
            val submit = compose.onNodeWithText(compose.activity.getString(R.string.enrollment_dock_submit))
            submit.assertIsNotEnabled().performTouchInput { click() }
            compose.waitForIdle()
            assertEquals(0, serverState().getJSONObject("requestCounts").optInt("POST /api/enrollment/submit"))
            assertTrue(serverState().isNull("submitted"))
        }
    }

    // Expected values follow contracts/v1/disciplines.json at the fixed clock:
    // allowances floor 25% of the hours, partial means exclude the Prova
    // Final, past semesters weigh grades by the hours of the offer taken, and
    // the CR counts a failure posted without a mean as 0.
    private fun runDisciplines() {
        open("unes://classes")
        waitFor(R.string.disciplines_tab_current)
        when (scenario) {
            "disciplines.current" -> {
                waitForText(quantity(R.plurals.disciplines_count, 4))
                waitForText("5,1")
                waitForText(quantity(R.plurals.disciplines_attention_title, 2))
                waitForText("Cálculo de Exemplo em prova final · Estatística de Exemplo com média baixa")
                assertCard("Algoritmos de Exemplo", string(R.string.disciplines_status_ongoing), "–", "13")
                assertCard("Cálculo de Exemplo", string(R.string.disciplines_status_final), "4,5", "11")
                assertCard("Física de Exemplo", string(R.string.disciplines_status_ongoing), "7,0", "15")
                assertCard("Estatística de Exemplo", string(R.string.disciplines_status_low), "4,0", "3")
            }
            "disciplines.history" -> {
                click(R.string.disciplines_tab_history)
                waitForText("2026.1")
                waitForText("4,2")
                waitForText("5,7")
                waitForText("33%")
                assertCard("Lógica de Exemplo", "8,5")
                assertCard("Probabilidade de Exemplo", "3,0")
                assertCard("Desenho de Exemplo", "–")
                capture("history")
                openCard("Probabilidade de Exemplo")
                waitForText(string(R.string.discipline_detail_media_final).uppercase(Locale.ROOT))
                waitFor(R.string.discipline_detail_status_failed)
                scrollListTo(hasText(string(R.string.discipline_detail_stat_hours)))
                waitForText(string(R.string.disciplines_card_hours_format, 60))
                compose.onAllNodesWithText(string(R.string.disciplines_card_hours_format, 75)).assertCountEquals(0)
                scrollListTo(hasText(string(R.string.discipline_detail_freq_of_format, 15)))
                waitForText(string(R.string.discipline_detail_freq_of_format, 15))
            }
            // Backfill tries the past semester once per session; waiting for
            // that attempt keeps it from downloading the semester behind the
            // retry once the server recovers.
            "disciplines.download-retry" -> {
                click(R.string.disciplines_tab_history)
                compose.waitUntil(90_000) {
                    serverState()
                        .getJSONObject("requestCounts")
                        .optInt("GET /api/sync/semesters/00000000-0000-4000-8000-000000000100") > 0
                }
                click(R.string.disciplines_undownloaded_cta)
                waitForText(string(R.string.disciplines_download_failed, "2026.1"))
                capture("download-failed")
                waitFor(R.string.disciplines_undownloaded_cta)
                selectScenario("disciplines.history")
                click(R.string.disciplines_undownloaded_cta)
                waitForText("5,7")
                assertCard("Probabilidade de Exemplo", "3,0")
            }
            "discipline.detail.finals" -> {
                assertCard("Cálculo de Exemplo", string(R.string.disciplines_status_final), "4,5")
                capture("list")
                openCard("Cálculo de Exemplo")
                waitForText(string(R.string.discipline_detail_media_partial).uppercase(Locale.ROOT))
                waitFor(R.string.discipline_detail_status_finals)
                waitForText("4,5")
                scrollListTo(hasText(string(R.string.discipline_detail_final_section)))
                scrollListTo(hasText(string(R.string.discipline_detail_callout_title)))
                scrollListTo(hasText("5,8"))
                waitForText("5,8")
                waitFor(R.string.discipline_detail_callout_min_caption)
            }
            "discipline.detail.attendance" -> {
                assertCard("Estatística de Exemplo", string(R.string.disciplines_status_low), "3")
                capture("list")
                openCard("Estatística de Exemplo")
                scrollListTo(hasText(string(R.string.discipline_detail_freq_warn_high)))
                waitForText("12")
                waitForText(string(R.string.discipline_detail_freq_of_format, 15))
                waitForText("3")
            }
            else -> error("Unsupported disciplines scenario: $scenario")
        }
    }

    // Discipline cards and history rows are clickable, so each merges its
    // texts into one node: matching them together pins values to one card.
    private fun assertCard(
        title: String,
        vararg values: String,
    ) {
        scrollListTo(hasText(title))
        val card = values.fold(hasText(title) and hasClickAction()) { matcher, value -> matcher and hasText(value) }
        compose.onNode(card).assertIsDisplayed()
    }

    private fun openCard(title: String) {
        scrollListTo(hasText(title))
        compose.onNode(hasText(title) and hasClickAction()).performClick()
        waitFor(R.string.discipline_detail_topbar_title)
    }

    // Lazy items outside the viewport have no semantics node until scrolled to.
    private fun scrollListTo(matcher: SemanticsMatcher) {
        val list =
            hasScrollToNodeAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
        compose.waitUntil(30_000) { compose.onAllNodes(list).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(list).onFirst().performScrollToNode(matcher)
    }

    private fun string(
        resource: Int,
        vararg args: Any,
    ) = compose.activity.getString(resource, *args)

    private fun quantity(
        resource: Int,
        count: Int,
    ) = compose.activity.resources.getQuantityString(resource, count, count)

    private fun scrollToText(text: String) {
        compose.waitUntil(30_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(text).onFirst().performScrollTo().assertIsDisplayed()
    }

    private fun scrollClick(text: String) {
        scrollToText(text)
        compose.onAllNodesWithText(text).onFirst().performClick()
    }

    private fun open(
        uri: String,
        refresh: Boolean = false,
    ) {
        compose.activity.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(uri), compose.activity, MainActivity::class.java).apply {
                if (refresh) putExtra("kind", "scenario-refresh")
            },
        )
    }

    // A launcher ANR from emulator boot can surface after CI's one-shot dialog
    // cleanup and hold the window focus Espresso waits for. Closing the app is
    // what hide_error_dialogs would have done.
    private fun awaitWindowFocus() {
        compose.waitUntil(30_000) {
            device.findObject(By.res("android", "aerr_close"))?.click()
            compose.runOnUiThread { compose.activity.hasWindowFocus() }
        }
    }

    private fun waitFor(resource: Int) = waitForText(compose.activity.getString(resource))

    private fun waitForText(text: String) {
        compose.waitUntil(90_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText(text).onFirst().assertIsDisplayed()
    }

    private fun click(resource: Int) {
        waitFor(resource)
        compose.onNodeWithText(compose.activity.getString(resource)).performClick()
    }

    private fun selectScenario(id: String) {
        connection("/debug/scenario/$id", "POST").apply {
            try {
                assertEquals(200, responseCode)
            } finally {
                disconnect()
            }
        }
    }

    private fun serverState(): JSONObject =
        connection("/debug/state", "GET").run {
            try {
                JSONObject(inputStream.bufferedReader().use { it.readText() }).getJSONObject("data")
            } finally {
                disconnect()
            }
        }

    private fun connection(
        path: String,
        method: String,
    ): HttpURLConnection =
        (URI("http://127.0.0.1:8787$path").toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 5_000
            readTimeout = 5_000
        }

    private fun capture(name: String) {
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "scenario-evidence").apply {
            mkdirs()
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val roots = compose.onAllNodes(isRoot(), useUnmergedTree = true)
        val rootCount = roots.fetchSemanticsNodes().size
        if (rootCount == 1) {
            File(directory, "$name.png").outputStream().use {
                assertTrue(
                    compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it),
                )
            }
        } else {
            assertTrue(device.takeScreenshot(File(directory, "$name.png")))
        }
        device.dumpWindowHierarchy(File(directory, "$name.xml"))
        File(directory, "$name-semantics.txt").writeText(
            (0 until rootCount).joinToString("\n") {
                roots[it].printToString()
            },
        )
        ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("logcat -d --pid ${Process.myPid()} -v threadtime"),
        ).use { input ->
            File(directory, "logcat.txt").outputStream().use { input.copyTo(it) }
        }
    }
}

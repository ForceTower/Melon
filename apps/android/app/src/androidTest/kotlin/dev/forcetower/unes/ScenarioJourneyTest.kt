package dev.forcetower.unes

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.Process
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import dev.forcetower.unes.remote.FeatureGates
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
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
        val initial = if (isEnrollment) {
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
        device.pressBack()
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
        if (isEnrollment) runEnrollment()
        assertEquals(
            "Unmocked requests must fail the journey",
            0,
            serverState().getJSONArray("unexpectedRequests").length(),
        )
    }

    @After
    fun collectEvidence() = capture("result")

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

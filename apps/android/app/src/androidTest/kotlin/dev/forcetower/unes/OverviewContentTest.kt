package dev.forcetower.unes

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.Density
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.ui.feature.overview.OverviewContent
import dev.forcetower.unes.ui.feature.overview.OverviewIntent
import dev.forcetower.unes.ui.feature.overview.OverviewUiState
import java.io.File
import java.util.Locale
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class OverviewContentTest(
    private val dark: Boolean,
    private val fontScale: Float,
) {
    @get:Rule
    val compose = createAndroidComposeRule<ScenarioRenderActivity>()

    @Test
    fun expiredSessionOffersRecoveryAndTakesPrecedenceOverCredentialFailure() {
        var selected: OverviewIntent? = null
        compose.enableAccessibilityChecks()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                MelonTheme(darkTheme = dark) {
                    OverviewContent(
                        state = OverviewUiState(
                            userName = "Estudante Exemplo",
                            clock = Instant.parse("2026-10-02T13:00:00Z"),
                            timeZone = TimeZone.of("America/Bahia"),
                            locale = Locale.forLanguageTag("pt-BR"),
                            sessionInvalid = true,
                            credentialsInvalid = true,
                        ),
                        onIntent = { selected = it },
                    )
                }
            }
        }
        compose.onNodeWithText("Bom dia, Estudante").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.credentials_banner_title)).assertDoesNotExist()
        compose.onNodeWithText(compose.activity.getString(R.string.overview_today_empty)).assertIsDisplayed()
        compose.onRoot().tryPerformAccessibilityChecks()
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        val directory = File(compose.activity.getExternalFilesDir(null), "scenario-evidence").apply { mkdirs() }
        File(directory, "home-session-expired-dark-$dark-font-$fontScale.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText(compose.activity.getString(R.string.session_expired_banner_title)).performClick()
        assertEquals(OverviewIntent.ReloginTapped, selected)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "dark={0}, fontScale={1}")
        fun configurations() = listOf(arrayOf<Any>(false, 1f), arrayOf<Any>(true, 1.3f))
    }
}

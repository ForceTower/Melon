package dev.forcetower.unes.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class HomeBenchmark {
    @get:Rule
    val benchmark = MacrobenchmarkRule()

    @Test
    fun coldStartWithCachedSemester() {
        benchmark.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.None(),
            startupMode = StartupMode.COLD,
            iterations = 10,
            setupBlock = {
                startActivityAndWait()
                device.signInIfNeeded()
                pressHome()
            },
        ) {
            startActivityAndWait()
            device.waitForHome()
        }
    }

    @Test
    fun homeScroll() {
        benchmark.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.None(),
            iterations = 5,
            setupBlock = {
                startActivityAndWait()
                device.signInIfNeeded()
                device.waitForHome()
            },
        ) {
            device.scrollHome()
        }
    }
}

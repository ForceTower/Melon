package dev.forcetower.unes.ui.feature.materials.upload

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialsError
import dev.forcetower.melon.feature.materials.domain.usecase.GetUploadSemestersUseCase
import dev.forcetower.melon.feature.materials.domain.usecase.SubmitMaterialUseCase
import dev.forcetower.unes.ui.feature.materials.MaterialsTestFixtures.calculus
import dev.forcetower.unes.ui.feature.materials.MaterialsTestFixtures.material
import dev.forcetower.unes.ui.feature.materials.MaterialsTestFixtures.physics
import dev.forcetower.unes.ui.feature.materials.settle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class MaterialsUploadViewModelTest {
    private val upload = CompletableDeferred<Outcome<Material, MaterialsError>>()
    private val resolver = mockk<ContentResolver>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        every { resolver.query(any(), any(), any(), any(), any()) } returns null
        every { resolver.openInputStream(any()) } answers { ByteArrayInputStream(byteArrayOf(1, 2, 3)) }
        every { resolver.openFileDescriptor(any(), any()) } throws FileNotFoundException()
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun anUploadThatLandsWithTheSheetOpenShowsSuccessAndRefetchesOnDone() =
        runTest {
            val model = model()
            val effects = effectsOf(model)
            model.onIntent(MaterialsUploadIntent.StartFromDiscipline(calculus))
            publishScan(model)

            upload.complete(Outcome.Ok(material("m1")))
            advanceUntilIdle()
            assertEquals(MaterialsUploadStep.Success, model.state.value.step)
            assertEquals(emptyList(), effects)

            model.onIntent(MaterialsUploadIntent.Dismiss)
            advanceUntilIdle()
            assertEquals(listOf<MaterialsUploadEffect>(MaterialsUploadEffect.Finished(calculus.id)), effects)
        }

    @Test
    fun closingTheSheetMidUploadStillRefetchesOnceItLands() =
        runTest {
            val model = model()
            val effects = effectsOf(model)
            model.onIntent(MaterialsUploadIntent.StartFromDiscipline(calculus))
            publishScan(model)

            model.onIntent(MaterialsUploadIntent.Dismiss)
            upload.complete(Outcome.Ok(material("m1")))
            advanceUntilIdle()

            assertEquals(listOf<MaterialsUploadEffect>(MaterialsUploadEffect.Finished(calculus.id)), effects)
        }

    @Test
    fun anUploadFromAClosedSheetLeavesTheReopenedOneAlone() =
        runTest {
            val model = model()
            model.onIntent(MaterialsUploadIntent.StartFromDiscipline(calculus))
            publishScan(model)
            model.onIntent(MaterialsUploadIntent.Dismiss)
            model.onIntent(MaterialsUploadIntent.StartFromHub(listOf(calculus, physics)))

            upload.complete(Outcome.Ok(material("m1")))
            advanceUntilIdle()

            val state = model.state.value
            assertEquals(MaterialsUploadStep.PickDiscipline, state.step)
            assertNull(state.submitted)
        }

    @Test
    fun aFailedUploadFromAClosedSheetDoesNotFlagTheReopenedOne() =
        runTest {
            val model = model()
            model.onIntent(MaterialsUploadIntent.StartFromDiscipline(calculus))
            publishScan(model)
            model.onIntent(MaterialsUploadIntent.Dismiss)
            model.onIntent(MaterialsUploadIntent.StartFromDiscipline(physics))

            upload.complete(Outcome.Err(MaterialsError.Connection))
            advanceUntilIdle()

            assertFalse(model.state.value.submitFailed)
        }

    @Test
    fun backDuringASlowFileReadStaysOnTheDisciplinePick() =
        runTest {
            val release = CountDownLatch(1)
            every { resolver.openInputStream(any()) } answers {
                release.await(5, TimeUnit.SECONDS)
                ByteArrayInputStream(byteArrayOf(1, 2, 3))
            }
            val model = model()
            model.onIntent(MaterialsUploadIntent.StartFromHub(listOf(calculus, physics)))
            model.onIntent(MaterialsUploadIntent.PickDiscipline(calculus.id))
            model.onIntent(MaterialsUploadIntent.FilePicked(mockk<Uri>()))
            advanceUntilIdle()

            model.onIntent(MaterialsUploadIntent.Back)
            release.countDown()
            settle(model)

            val state = model.state.value
            assertEquals(MaterialsUploadStep.PickDiscipline, state.step)
            assertNull(state.discipline)
            assertNull(state.file)
        }

    // Scan → details → "Continuar", which submits straight away because the
    // guidelines were acknowledged; the upload then waits on `upload`.
    private fun TestScope.publishScan(model: MaterialsUploadViewModel) {
        model.onIntent(MaterialsUploadIntent.ScanPicked(mockk<Uri>(), pages = 2))
        settle(model)
        model.onIntent(MaterialsUploadIntent.TitleChanged("Prova 1"))
        model.onIntent(MaterialsUploadIntent.SemesterChanged("2026.1"))
        model.onIntent(MaterialsUploadIntent.Continue)
        advanceUntilIdle()
    }

    private fun TestScope.effectsOf(model: MaterialsUploadViewModel): List<MaterialsUploadEffect> {
        val effects = mutableListOf<MaterialsUploadEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.effects.toList(effects) }
        return effects
    }

    private fun model(): MaterialsUploadViewModel {
        val prefs = mockk<SharedPreferences>()
        every { prefs.getBoolean(any(), any()) } returns true
        every { prefs.edit() } returns mockk(relaxed = true)
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { context.contentResolver } returns resolver
        val submitMaterial = mockk<SubmitMaterialUseCase>()
        coEvery { submitMaterial(any()) } coAnswers { upload.await() }
        val getUploadSemesters = mockk<GetUploadSemestersUseCase>()
        coEvery { getUploadSemesters() } returns emptyList()
        return MaterialsUploadViewModel(
            context = context,
            submitMaterial = submitMaterial,
            getUploadSemesters = getUploadSemesters,
            analytics = NoOpAnalytics,
        )
    }
}

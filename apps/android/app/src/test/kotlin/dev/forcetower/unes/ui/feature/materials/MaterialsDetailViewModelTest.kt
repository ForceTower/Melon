package dev.forcetower.unes.ui.feature.materials

import android.content.Context
import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.domain.model.FetchedMaterialFile
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialsError
import dev.forcetower.melon.feature.materials.domain.usecase.GetMaterialUseCase
import dev.forcetower.melon.feature.materials.domain.usecase.OpenMaterialUseCase
import dev.forcetower.melon.feature.materials.domain.usecase.SetMaterialSavedUseCase
import dev.forcetower.melon.feature.materials.domain.usecase.SetMaterialUsefulUseCase
import dev.forcetower.unes.ui.feature.materials.MaterialsTestFixtures.material
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

// One view model serves every detail screen, so work started on one material
// must not land on the next one the student opens.
@OptIn(ExperimentalCoroutinesApi::class)
internal class MaterialsDetailViewModelTest {
    private val cacheDir = Files.createTempDirectory("materials-cache").toFile()
    private val getMaterial = mockk<GetMaterialUseCase>()
    private val setUseful = mockk<SetMaterialUsefulUseCase>()
    private val setSaved = mockk<SetMaterialSavedUseCase>()
    private val openMaterial = mockk<OpenMaterialUseCase>()

    private val left = material("a", usefulCount = 3, downloadCount = 10, isUseful = false, isSaved = false)
    private val shown = material("b", usefulCount = 7, downloadCount = 20, isUseful = true, isSaved = true)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
        cacheDir.deleteRecursively()
    }

    @Test
    fun openingTheFileOnScreenCountsTheDownloadAndViewsIt() =
        runTest {
            coEvery { openMaterial(left.id) } returns Outcome.Ok(FetchedMaterialFile(byteArrayOf(1), "a.pdf"))
            val model = model()
            val effects = effectsOf(model)
            model.onIntent(MaterialsDetailIntent.Seed(left))

            model.onIntent(MaterialsDetailIntent.OpenFile)
            settle(model)

            assertEquals(left.downloadCount + 1, model.state.value.material?.downloadCount)
            assertEquals(1, effects.filterIsInstance<MaterialsDetailEffect.ViewFile>().size)
        }

    @Test
    fun aDownloadFinishingAfterLeavingDoesNotTouchOrOpenOverTheNextMaterial() =
        runTest {
            val download = CompletableDeferred<Outcome<FetchedMaterialFile, MaterialsError>>()
            coEvery { openMaterial(left.id) } coAnswers { download.await() }
            val model = model()
            val effects = effectsOf(model)
            model.onIntent(MaterialsDetailIntent.Seed(left))
            model.onIntent(MaterialsDetailIntent.OpenFile)
            advanceUntilIdle()

            model.onIntent(MaterialsDetailIntent.Seed(shown))
            download.complete(Outcome.Ok(FetchedMaterialFile(byteArrayOf(1), "a.pdf")))
            settle(model)

            assertEquals(shown, model.state.value.material)
            assertEquals(emptyList(), effects)
        }

    @Test
    fun aFailedUsefulVoteAfterLeavingDoesNotRollBackTheNextMaterial() =
        runTest {
            val vote = CompletableDeferred<Outcome<Int, MaterialsError>>()
            coEvery { setUseful(left.id, true) } coAnswers { vote.await() }
            val model = model()
            model.onIntent(MaterialsDetailIntent.Seed(left))
            model.onIntent(MaterialsDetailIntent.ToggleUseful)
            advanceUntilIdle()

            model.onIntent(MaterialsDetailIntent.Seed(shown))
            vote.complete(Outcome.Err(MaterialsError.Connection))
            advanceUntilIdle()

            assertEquals(shown, model.state.value.material)
        }

    @Test
    fun aFailedSaveAfterLeavingDoesNotRollBackTheNextMaterial() =
        runTest {
            val save = CompletableDeferred<Outcome<Unit, MaterialsError>>()
            coEvery { setSaved(left.id, true) } coAnswers { save.await() }
            val model = model()
            model.onIntent(MaterialsDetailIntent.Seed(left))
            model.onIntent(MaterialsDetailIntent.ToggleSave)
            advanceUntilIdle()

            model.onIntent(MaterialsDetailIntent.Seed(shown))
            save.complete(Outcome.Err(MaterialsError.Connection))
            advanceUntilIdle()

            assertEquals(shown, model.state.value.material)
        }

    @Test
    fun aSlowDeeplinkFetchDoesNotReplaceTheMaterialSeededSince() =
        runTest {
            val fetch = CompletableDeferred<Outcome<Material, MaterialsError>>()
            coEvery { getMaterial(left.id) } coAnswers { fetch.await() }
            coEvery { getMaterial(shown.id) } returns Outcome.Ok(shown)
            val model = model()
            model.onIntent(MaterialsDetailIntent.Ensure(left.id))
            advanceUntilIdle()

            model.onIntent(MaterialsDetailIntent.Seed(shown))
            model.onIntent(MaterialsDetailIntent.Ensure(shown.id))
            fetch.complete(Outcome.Ok(left))
            advanceUntilIdle()

            assertEquals(shown, model.state.value.material)
        }

    private fun TestScope.effectsOf(model: MaterialsDetailViewModel): List<MaterialsDetailEffect> {
        val effects = mutableListOf<MaterialsDetailEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.effects.toList(effects) }
        return effects
    }

    private fun model(): MaterialsDetailViewModel {
        val context = mockk<Context>()
        every { context.cacheDir } returns cacheDir
        return MaterialsDetailViewModel(
            context = context,
            getMaterial = getMaterial,
            setUseful = setUseful,
            setSaved = setSaved,
            reportMaterial = mockk(),
            openMaterial = openMaterial,
            analytics = NoOpAnalytics,
            reviewPrompter = mockk(relaxed = true),
        )
    }
}

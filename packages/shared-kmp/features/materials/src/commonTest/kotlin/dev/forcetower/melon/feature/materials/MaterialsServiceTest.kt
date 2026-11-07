package dev.forcetower.melon.feature.materials

import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.materials.data.network.MaterialsService
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialsError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class MaterialsServiceTest {
    // The detail screen cancels a deeplink fetch when the student opens
    // another material; an Err returned to the cancelled fetch used to mark
    // that other material as failed to load.
    @Test
    fun aCancelledRequestEndsTheCallerInsteadOfReportingAConnectionError() =
        runTest {
            val config = MockEngineConfig().apply {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler { awaitCancellation() }
            }
            val engine = MockEngine(config)
            val service = MaterialsService(HttpClient(engine) { expectSuccess = false }, engine)
            var outcome: Outcome<Material, MaterialsError>? = null

            val fetch = launch { outcome = service.material("a") }
            runCurrent()
            fetch.cancelAndJoin()

            assertNull(outcome)
        }
}

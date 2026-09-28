package dev.forcetower.melon.feature.auth

import dev.forcetower.melon.feature.auth.data.dto.LoginResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

internal class PilotLoginContractTest {
    @Test
    fun sharedProviderLoginExampleDecodesWithoutRealCredentials() {
        val json = Json { ignoreUnknownKeys = true }
        val fixture = javaClass.getResourceAsStream("/v1/pilot.json")!!.bufferedReader().use {
            json.parseToJsonElement(it.readText()).jsonObject
        }
        val response = json.decodeFromJsonElement<LoginResponse>(fixture.getValue("login"))
        assertEquals("Estudante Exemplo", response.user.name)
        assertEquals("synthetic-access-0", response.accessToken)
        assertEquals("synthetic-refresh-0", response.refreshToken)
    }
}

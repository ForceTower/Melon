package dev.forcetower.melon.core.sync

import dev.forcetower.melon.core.sync.data.dto.SemesterPayloadResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

internal class DisciplinesContractTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixture = javaClass.getResourceAsStream("/v1/disciplines.json")!!.bufferedReader().use {
        json.parseToJsonElement(it.readText()).jsonObject
    }

    @Test
    fun currentAndPastSemestersDecodeIntoTheNativeMirror() {
        val current = json.decodeFromJsonElement<SemesterPayloadResponse>(fixture.getValue("current"))
        val past = json.decodeFromJsonElement<SemesterPayloadResponse>(fixture.getValue("past"))
        assertEquals("2026.2", current.semester.code)
        assertEquals(4, current.studentClasses.size)
        val final = current.studentGrades.single { it.name == "Prova Final" }
        assertEquals("Adicional", final.nameShort)
        assertNull(final.value)
        assertEquals("2026.1", past.semester.code)
        val drifted = past.disciplines.single { it.code == "DEMO410" }
        assertEquals(75, drifted.hours)
        assertEquals(60, past.disciplineOffers.single { it.disciplineId == drifted.id }.hours)
    }
}

package dev.forcetower.melon.core.sync

import dev.forcetower.melon.core.sync.data.dto.MessagePageResponse
import dev.forcetower.melon.core.sync.data.dto.OnboardingStatusResponse
import dev.forcetower.melon.core.sync.data.dto.ProfileResponse
import dev.forcetower.melon.core.sync.data.dto.SemesterPayloadResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class PilotContractTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val fixture = javaClass.getResourceAsStream("/v1/pilot.json")!!.bufferedReader().use {
        json.parseToJsonElement(it.readText()).jsonObject
    }

    @Test
    fun sharedProviderExampleDecodesIntoTheNativeMirror() {
        val profile = json.decodeFromJsonElement<ProfileResponse>(fixture.getValue("profile"))
        val semester = json.decodeFromJsonElement<SemesterPayloadResponse>(fixture.getValue("semester"))
        val onboarding = json.decodeFromJsonElement<OnboardingStatusResponse>(fixture.getValue("onboarding"))
        val messages = json.decodeFromJsonElement<MessagePageResponse>(fixture.getValue("messages"))
        assertEquals("Estudante Exemplo", profile.user.name)
        assertEquals(profile.course?.id, profile.student.courseId)
        assertEquals("2026.2", semester.semester.code)
        assertEquals("Algoritmos de Exemplo", semester.disciplines.single().name)
        assertEquals(5, semester.allocations.single().day)
        assertEquals("10:00", semester.allocations.single().startTime)
        assertEquals(semester.classes.single().id, semester.studentClasses.single().classId)
        assertEquals(semester.semester.id, semester.disciplineOffers.single().semesterId)
        assertTrue(onboarding.activeSemesterReady)
        assertEquals(1, onboarding.initial?.appliedSemesters)
        assertTrue(messages.messages.isEmpty())
        assertEquals(null, messages.nextCursor)
    }
}

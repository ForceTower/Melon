package dev.forcetower.melon.core.sync

import dev.forcetower.melon.core.sync.data.dto.MessagePageResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

// The populated inbox page the notification journeys serve must decode into
// the same DTO the mirror applies, or the scenario would test a shape the
// app never receives.
internal class MessagesContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun notificationInboxPageDecodesIntoTheNativeMirror() {
        val fixture = javaClass.getResourceAsStream("/v1/messages.json")!!.bufferedReader().use {
            json.parseToJsonElement(it.readText()).jsonObject
        }
        val page = json.decodeFromJsonElement<MessagePageResponse>(fixture.getValue("page"))
        val message = page.messages.single()
        assertEquals("6d5c4b3a-2f1e-4d0c-9b8a-7f6e5d4c3b2a", message.id)
        assertEquals("app", message.source)
        assertEquals("Aviso de Exemplo", message.subject)
        assertEquals("2026-10-02T12:30:00Z", message.timestamp)
        assertFalse(message.read)
        assertEquals(null, page.nextCursor)
    }
}

package dev.forcetower.unes.reminders

import android.content.Context
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Wire-format snapshot the host process writes and the alarm receiver reads
// back at fire time — same disk-handoff pattern as `EvaluationReminderSnapshot`,
// so the receiver never needs a database connection. Entries that asked for no
// reminder never reach here.
@Serializable
internal data class PersonalEventReminderSnapshot(
    val reminders: List<Entry>,
) {
    @Serializable
    data class Entry(
        val id: String,
        val title: String,
        // "YYYY-MM-DD" — the day the alarm should fire, already offset by the
        // student's chosen lead time.
        val fireDateIso: String,
    )

    companion object {
        const val FILE_NAME = "personal-event-reminders.json"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun file(context: Context): File = File(context.filesDir, FILE_NAME)

        fun load(context: Context): PersonalEventReminderSnapshot? {
            val f = file(context)
            if (!f.exists()) return null
            return runCatching {
                json.decodeFromString(serializer(), f.readText())
            }.getOrNull()
        }

        fun save(context: Context, snapshot: PersonalEventReminderSnapshot) {
            val f = file(context)
            // Atomic replace: tmp sibling + rename so a half-written JSON never
            // reaches the receiver.
            val tmp = File(f.parentFile, "${f.name}.tmp")
            tmp.writeText(json.encodeToString(serializer(), snapshot))
            if (!tmp.renameTo(f)) {
                f.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }
}

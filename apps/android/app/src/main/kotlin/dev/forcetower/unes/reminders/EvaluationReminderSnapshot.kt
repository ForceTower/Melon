package dev.forcetower.unes.reminders

import android.content.Context
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Wire-format snapshot the host process writes and the alarm receiver reads
// back at fire time — same disk-handoff pattern as `WidgetSnapshot`, so the
// receiver never needs a database connection. Only already-gated reminders
// land here: a disabled switch or an empty upcoming set writes an empty
// snapshot, which also cancels the pending alarm.
@Serializable
internal data class EvaluationReminderSnapshot(
    val reminders: List<Entry>,
) {
    @Serializable
    data class Entry(
        // Stable across sync's wipe-then-insert (discipline + platform grade id).
        val key: String,
        // "P2", "Prova Final", …
        val label: String,
        val disciplineName: String,
        // "YYYY-MM-DD" — the evaluation day; the alarm fires the evening before.
        val dateIso: String,
    )

    companion object {
        const val FILE_NAME = "evaluation-reminders.json"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun file(context: Context): File = File(context.filesDir, FILE_NAME)

        fun load(context: Context): EvaluationReminderSnapshot? {
            val f = file(context)
            if (!f.exists()) return null
            return runCatching {
                json.decodeFromString(serializer(), f.readText())
            }.getOrNull()
        }

        fun save(context: Context, snapshot: EvaluationReminderSnapshot) {
            val f = file(context)
            // Atomic replace, like WidgetSnapshot: tmp sibling + rename so a
            // half-written JSON never reaches the receiver.
            val tmp = File(f.parentFile, "${f.name}.tmp")
            tmp.writeText(json.encodeToString(serializer(), snapshot))
            if (!tmp.renameTo(f)) {
                f.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }
}

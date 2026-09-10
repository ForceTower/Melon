package dev.forcetower.melon.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import dev.forcetower.melon.core.database.entity.AcademicCalendarEventEntity
import dev.forcetower.melon.core.database.entity.SettingsEntity
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RoomMigrationTest {
    @Test
    fun opensExistingRoom2DatabaseWithoutLosingData() = runBlocking {
        withDatabase(14) { database ->
            assertEquals("dark", database.settingsDao().get("theme"))
            assertEquals("Study session", database.personalEventDao().all().single().title)
            database.settingsDao().put(SettingsEntity("theme", "light"))
            assertEquals("light", database.settingsDao().observe("theme").first())
        }
    }

    @Test
    fun migratesEverySupportedRoom2SchemaWithoutLosingData() = runBlocking {
        for (version in 8..13) {
            withDatabase(version) { database ->
                assertEquals("dark", database.settingsDao().get("theme"), "Schema $version")
                val personalEvents = database.personalEventDao().all()
                if (version >= 9) {
                    assertEquals("Study session", personalEvents.single().title, "Schema $version")
                } else {
                    assertEquals(emptyList(), personalEvents)
                }
                assertEquals(emptyList(), database.curriculumDao().observeVersions().first())
            }
        }
    }

    @Test
    fun replacesCalendarEventsWithinATransaction() = runBlocking {
        withDatabase(14) { database ->
            val dao = database.calendarEventDao()
            val oldEvent = calendarEvent("old")
            val newEvent = calendarEvent("new")
            dao.replaceAll(listOf(oldEvent))
            dao.replaceAll(listOf(newEvent))
            assertEquals(listOf(newEvent), dao.observeAll().first())
            dao.replaceAll(emptyList())
            assertEquals(emptyList(), dao.observeAll().first())
        }
    }

    private suspend fun withDatabase(version: Int, block: suspend (MelonDatabase) -> Unit) {
        val directory = Files.createTempDirectory("melon-room-migration").toFile()
        try {
            val file = File(directory, "melon.db")
            createRoom2Database(file, version)
            val database = Room.databaseBuilder<MelonDatabase>(name = file.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            try {
                block(database)
            } finally {
                database.close()
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun createRoom2Database(file: File, version: Int) {
        val resource = "/dev.forcetower.melon.core.database.MelonDatabase/$version.json"
        val schema = requireNotNull(javaClass.getResourceAsStream(resource)).bufferedReader().use {
            Json.parseToJsonElement(it.readText()).jsonObject.getValue("database").jsonObject
        }
        BundledSQLiteDriver().open(file.absolutePath).use { connection ->
            for (element in schema.getValue("entities").jsonArray) {
                val entity = element.jsonObject
                val tableName = entity.getValue("tableName").jsonPrimitive.content
                val createSql = entity.getValue("createSql").jsonPrimitive.content
                connection.execSQL(createSql.replace("\${TABLE_NAME}", tableName))
                for (index in entity["indices"]?.jsonArray.orEmpty()) {
                    val sql = index.jsonObject.getValue("createSql").jsonPrimitive.content
                    connection.execSQL(sql.replace("\${TABLE_NAME}", tableName))
                }
            }
            for (query in schema.getValue("setupQueries").jsonArray) {
                connection.execSQL(query.jsonPrimitive.content)
            }
            connection.execSQL("PRAGMA user_version = $version")
            connection.execSQL("INSERT INTO Settings (`key`, value) VALUES ('theme', 'dark')")
            if (version >= 9) {
                connection.execSQL(
                    """
                    INSERT INTO PersonalEvent (id, title, start, category, reminderDays, notes, createdAt)
                    VALUES ('personal', 'Study session', '2026-09-14', 'STUDY', 0, '', 1)
                    """.trimIndent(),
                )
            }
        }
    }

    private fun calendarEvent(id: String) = AcademicCalendarEventEntity(
        id = id,
        platformId = id,
        description = "Academic event",
        start = "2026-09-14",
        end = null,
        fixed = false,
        closed = false,
        scope = "GENERAL",
        origin = "MANUAL",
    )
}

package com.forcetower.uefs.core.task

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.task.definers.DisciplinesProcessor
import com.forcetower.uefs.core.task.definers.SemestersProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.breaker.Orchestra
import dev.forcetower.breaker.model.Authorization
import dev.forcetower.breaker.model.Semester
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import timber.log.Timber

class FetchMissingSemestersUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: UDatabase,
    private val client: OkHttpClient,
    @Named("webViewUA") private val agent: String,
    @Named("flagSnowpiercerEnabled") private val snowpiercerEnabled: Boolean,
    @Named("internalConfig") private val config: DataStore<Preferences>
) : UseCase<Unit, Unit>(Dispatchers.IO) {
    private val key = stringSetPreferencesKey("missing_semesters_run")
    override suspend fun execute(parameters: Unit) {
        if (!snowpiercerEnabled) return

        val access = database.accessDao().getAccessDirect()
        val profile = database.profileDao().selectMeDirect()
        access ?: return
        profile ?: return

        val orchestra = Orchestra.Builder().client(client).userAgent(agent).build()
        orchestra.setAuthorization(Authorization(access.username, access.password))

        val semesters = database.semesterDao().getSemestersDirect()
        val missing = basicSemesters.filter { semesters.none { s -> s.sagresId == it.id } }

        missing.forEach { semester ->
            val set = config.data.firstOrNull()?.get(key) ?: emptySet()
            if (set.contains(semester.id.toString())) return@forEach
            val result = orchestra.grades(profile.sagresId, semester.id)

            result.success()?.value?.let {
                if (it.isNotEmpty()) {
                    SemestersProcessor(database, listOf(semester)).execute()
                    val currentSemesterIns = database.semesterDao().getSemesterDirect(semester.id)!!
                    DisciplinesProcessor(context, database, it, currentSemesterIns.uid, profile.sagresId, false).execute()
                }
                config.edit { prefs ->
                    val pref = prefs[key] ?: emptySet()
                    prefs[key] = pref.toMutableSet().also { s -> s.add(semester.id.toString()) }
                }
            }
            result.error()?.let {
                Timber.e("Failed to get all grades from semester ${semester.code}")
            }
        }
    }

    companion object {
        private val basicSemesters = listOf(
            Semester(1000000792, "20181", "20181", "2018-03-19T00:00:00-03:00", "2018-08-08T00:00:00-03:00"),
            Semester(1000000754, "20172", "20172", "2017-09-11T00:00:00-03:00", "2018-02-21T00:00:00-03:00"),
            Semester(1000000713, "20171", "20171", "2017-03-13T00:00:00-03:00", "2017-08-19T00:00:00-03:00"),
            Semester(1000000679, "20152 F", "20152 - Extra", "2016-05-30T00:00:00-03:00", "2016-07-01T00:00:00-03:00"),
            Semester(1000000623, "20152", "20152", "2015-11-20T00:00:00-02:00", "2016-05-16T00:00:00-03:00"),
            Semester(1000000594, "20151", "20151", "2015-03-03T00:00:00-03:00", "2015-10-29T00:00:00-02:00"),
            Semester(1000000553, "20142", "2014.2", "2014-08-25T00:00:00-03:00", "2014-12-23T00:00:00-02:00"),
            Semester(1000000486, "20141", "2014.1", "2014-03-10T00:00:00-03:00", "2014-08-02T00:00:00-03:00"),
            Semester(1000000532, "20132 F", "20132-Férias", "2014-01-14T00:00:00-02:00", "2014-02-13T00:00:00-02:00"),
            Semester(1000000483, "20132", "2013.2", "2013-08-26T00:00:00-03:00", "2014-01-06T00:00:00-02:00"),
            Semester(1000000439, "20131", "2013.1", "2013-03-11T00:00:00-03:00", "2013-08-15T00:00:00-03:00"),
            Semester(1000000403, "20122", "2012.2", "2012-09-05T00:00:00-03:00", "2013-01-23T00:00:00-02:00"),
            Semester(1000000372, "20121", "2012.1", "2012-04-09T00:00:00-03:00", "2012-08-27T00:00:00-03:00"),
            Semester(1000000340, "20112", "2011.2", "2011-09-29T00:00:00-03:00", "2012-03-19T00:00:00-03:00")
        )
    }
}

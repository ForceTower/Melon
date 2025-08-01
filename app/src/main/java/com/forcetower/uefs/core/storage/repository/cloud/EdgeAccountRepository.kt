package com.forcetower.uefs.core.storage.repository.cloud

import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.forcetower.uefs.core.model.edge.account.ChangePictureDTO
import com.forcetower.uefs.core.model.edge.auth.EdgeLoginBody
import com.forcetower.uefs.core.model.unes.EdgeAccessToken
import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.EdgeService
import com.google.gson.JsonParser
import dagger.Reusable
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import retrofit2.HttpException
import timber.log.Timber

@Reusable
class EdgeAccountRepository @Inject constructor(
    private val service: EdgeService,
    private val database: UDatabase,
    private val preferences: SharedPreferences
) {
    fun getAccount() = database.edgeServiceAccount.me().filterNotNull()

    suspend fun fetchAccountIfNeeded() {
        database.edgeAccessToken.require() ?: return
        Timber.d("Has edge token")

        val me = service.me().data
        val value = EdgeServiceAccount(
            id = me.id,
            name = me.name,
            email = me.email,
            imageUrl = me.imageUrl,
            me = true
        )
        database.edgeServiceAccount.insertOrUpdate(value)

        val student = service.studentMe().data
        // This is a hack!
        preferences.edit { putString("edge_course_id", student.courseId) }
        if (student.courseId == "5c6d29fd-9de3-4140-acf2-dc5847942e32") {
            database.profileDao().updateCourse(1)
        }
    }

    suspend fun startSession() {
        val token = database.edgeAccessToken.require() ?: return
        Timber.d("Has edge token")
        val access = database.accessDao().getAccessDirectSuspend() ?: return
        Timber.d("No credentials")
        runCatching {
            service.sessionStart(EdgeLoginBody(access.username, access.password))
        }.onFailure { error ->
            if (error is HttpException && error.code() == 401) {
                handleHttpException(token)
            } else {
                Timber.e(error, "Failed to start edge session. Cookie wont be validated.")
            }
        }
    }

    private suspend fun handleHttpException(token: EdgeAccessToken) {
        val accessToken = token.accessToken
        val parts = accessToken.split(".")
        if (parts.size != 3) {
            Timber.i("Failed to split token into parts. Size is not valid: ${parts.size}")
            return
        }

        val payloadStr = parts[1]
        val payloadDecoded = Base64.decode(payloadStr, Base64.DEFAULT).decodeToString()
        val payload = runCatching { JsonParser.parseString(payloadDecoded) }.getOrNull()

        if (payload == null) {
            Timber.i("Failed to decode auth payload")
            return
        }

        if (!payload.isJsonObject) {
            Timber.e("Failed to convert payload to json object")
            return
        }

        val obj = payload.asJsonObject
        if (!obj.has("exp")) {
            Timber.e("Expiration not found!")
            return
        }

        val exp = runCatching { obj.get("exp").asLong }.getOrNull()

        if (exp == null) {
            Timber.e("Expiration is not a long.")
            return
        }

        val now = System.currentTimeMillis() / 1000
        if (now < exp) {
            Timber.e("Failed for other reason. Token is not expired.")
            return
        }

        Timber.d("Refreshing token.")

        database.edgeAccessToken.deleteAll()
        prepareAndLogin()
    }

    private suspend fun prepareAndLogin() {
        val access = database.accessDao().getAccessDirectSuspend()
        if (access == null) {
            Timber.i("Access is null! How?")
            return
        }

        if (!access.valid) {
            Timber.i("Access is not valid. Skipping!")
            return
        }

        val result = service.loginAnonymous(EdgeLoginBody(access.username, access.password))
        Timber.i("Login completed with result $result")
        database.edgeAccessToken.insert(EdgeAccessToken(result.accessToken))
    }

    suspend fun uploadPicture(base64: String) {
        service.uploadPicture(ChangePictureDTO(base64))
        runCatching { fetchAccountIfNeeded() }
    }
}

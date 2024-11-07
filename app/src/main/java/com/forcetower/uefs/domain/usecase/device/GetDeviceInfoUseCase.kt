package com.forcetower.uefs.domain.usecase.device

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@Reusable
class GetDeviceInfoUseCase @Inject constructor(
    @Named("settings") private val settings: DataStore<Preferences>
) {
    private val deviceId = stringPreferencesKey("device_id")

    @Inject
    fun generateMachineId(): String {
        return runBlocking {
            settings.data.first()[deviceId] ?: run {
                val generated = UUID.randomUUID().toString()
                settings.edit { it[deviceId] = generated }
                generated
            }
        }
    }

    fun machineIdDirect(): String {
        return runBlocking {
            settings.data.first()[deviceId]?.let { str ->
                val bytes = MessageDigest.getInstance("MD5").digest(str.toByteArray(Charsets.UTF_8))
                bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
            } ?: UNINITIALIZED
        }
    }

    fun machineId(): Flow<String> {
        return settings.data.map {
            it[deviceId]?.let { str ->
                val bytes = MessageDigest.getInstance("MD5").digest(str.toByteArray(Charsets.UTF_8))
                bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
            } ?: UNINITIALIZED
        }
    }

    private companion object {
        const val UNINITIALIZED = "unavailable"
    }
}
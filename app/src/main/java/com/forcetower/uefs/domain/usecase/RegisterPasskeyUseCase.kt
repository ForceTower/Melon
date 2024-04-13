package com.forcetower.uefs.domain.usecase

import com.forcetower.uefs.core.model.edge.PasskeyRegister
import com.forcetower.uefs.core.model.edge.RegisterPasskeyStart
import com.forcetower.uefs.core.storage.repository.cloud.EdgeAuthRepository
import com.google.gson.Gson
import dagger.Reusable
import timber.log.Timber
import javax.inject.Inject

@Reusable
class RegisterPasskeyUseCase @Inject constructor(
    private val edge: EdgeAuthRepository,
    private val gson: Gson
) {
    suspend fun start(): RegisterPasskeyStart {
        val data = edge.registerStart()
        Timber.d("Original data: ${data.create}")
        val register = gson.fromJson(data.create, PasskeyRegister::class.java)
        return data.copy(create = gson.toJson(register.publicKey))
    }

    suspend fun finish(flowId: String, credential: String) {
        return edge.registerFinish(flowId, credential)
    }
}

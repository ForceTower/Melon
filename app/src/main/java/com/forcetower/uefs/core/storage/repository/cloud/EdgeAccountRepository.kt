package com.forcetower.uefs.core.storage.repository.cloud

import com.forcetower.uefs.core.model.edge.account.ChangePictureDTO
import com.forcetower.uefs.core.model.unes.EdgeServiceAccount
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.storage.network.EdgeService
import dagger.Reusable
import javax.inject.Inject
import timber.log.Timber

@Reusable
class EdgeAccountRepository @Inject constructor(
    private val service: EdgeService,
    private val database: UDatabase
) {
    fun getAccount() = database.edgeServiceAccount.me()

    suspend fun fetchAccountIfNeeded() {
        val token = database.edgeAccessToken.require() ?: return
        Timber.d("Has edge token $token")

        val me = service.me().data
        val value = EdgeServiceAccount(
            id = me.id,
            name = me.name,
            email = me.email,
            imageUrl = me.imageUrl,
            me = true
        )
        database.edgeServiceAccount.insertOrUpdate(value)
    }

    suspend fun uploadPicture(base64: String) {
        service.uploadPicture(ChangePictureDTO(base64))
        runCatching { fetchAccountIfNeeded() }
    }
}

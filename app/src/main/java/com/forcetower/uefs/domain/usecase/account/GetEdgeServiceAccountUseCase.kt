package com.forcetower.uefs.domain.usecase.account

import com.forcetower.uefs.core.storage.repository.cloud.EdgeAccountRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetEdgeServiceAccountUseCase @Inject constructor(
    private val repository: EdgeAccountRepository
) {
    operator fun invoke() = repository.getAccount()
    suspend fun update() = repository.fetchAccountIfNeeded()
}

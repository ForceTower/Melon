package com.forcetower.uefs.domain.usecase.profile

import com.forcetower.uefs.core.storage.repository.ProfileRepository
import dagger.Reusable
import javax.inject.Inject

@Reusable
class GetProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke() = repository.me()
}

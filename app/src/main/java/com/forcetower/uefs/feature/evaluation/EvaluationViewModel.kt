package com.forcetower.uefs.feature.evaluation

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.storage.repository.AccountRepository
import com.forcetower.uefs.core.storage.repository.EvaluationRepository
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.storage.resource.Resource
import javax.inject.Inject

class EvaluationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val evaluationRepository: EvaluationRepository
) : ViewModel() {
    private var _trending: LiveData<Resource<List<EvaluationHomeTopic>>>? = null

    fun getToken() = authRepository.getAccessToken()
    fun getAccount() = accountRepository.getAccount()
    fun getTrendingList(): LiveData<Resource<List<EvaluationHomeTopic>>> {
        if (_trending == null) {
            _trending = evaluationRepository.getTrendingList()
        }
        return _trending!!
    }
}
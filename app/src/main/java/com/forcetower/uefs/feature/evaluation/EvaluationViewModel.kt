package com.forcetower.uefs.feature.evaluation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.service.EvaluationDiscipline
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.storage.repository.AccountRepository
import com.forcetower.uefs.core.storage.repository.EvaluationRepository
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.evaluation.home.HomeInteractor
import javax.inject.Inject

class EvaluationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val evaluationRepository: EvaluationRepository
) : ViewModel(), HomeInteractor {
    private var _trending: LiveData<Resource<List<EvaluationHomeTopic>>>? = null
    private val _disciplineSelect = MutableLiveData<Event<EvaluationDiscipline>>()
    val disciplineSelect: LiveData<Event<EvaluationDiscipline>>
        get() = _disciplineSelect

    private var _discipline: LiveData<Resource<EvaluationDiscipline>>? = null

    fun getToken() = authRepository.getAccessToken()
    fun getAccount() = accountRepository.getAccount()
    fun getTrendingList(): LiveData<Resource<List<EvaluationHomeTopic>>> {
        if (_trending == null) {
            _trending = evaluationRepository.getTrendingList()
        }
        return _trending!!
    }

    override fun onClickDiscipline(discipline: EvaluationDiscipline) {
        _disciplineSelect.value = Event(discipline)
    }

    fun getDiscipline(department: String, code: String): LiveData<Resource<EvaluationDiscipline>> {
        if (_discipline == null) {
            _discipline = evaluationRepository.getDiscipline(department, code)
        }
        return _discipline!!
    }
}
package com.forcetower.uefs.core.storage.repository

import androidx.lifecycle.LiveData
import com.forcetower.uefs.AppExecutors
import com.forcetower.uefs.core.model.service.EvaluationHomeTopic
import com.forcetower.uefs.core.storage.network.UService
import com.forcetower.uefs.core.storage.network.adapter.asLiveData
import com.forcetower.uefs.core.storage.resource.NetworkOnlyResource
import com.forcetower.uefs.core.storage.resource.Resource
import javax.inject.Inject

class EvaluationRepository @Inject constructor(
    private val service: UService,
    private val executors: AppExecutors
) {
    fun getTrendingList(): LiveData<Resource<List<EvaluationHomeTopic>>> {
        return object : NetworkOnlyResource<List<EvaluationHomeTopic>>(executors) {
            override fun createCall() = service.getEvaluationTopics().asLiveData()
            override fun saveCallResult(value: List<EvaluationHomeTopic>) = Unit
        }.asLiveData()
    }
}

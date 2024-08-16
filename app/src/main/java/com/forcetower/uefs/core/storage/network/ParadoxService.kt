package com.forcetower.uefs.core.storage.network

import com.forcetower.uefs.core.model.edge.ServiceResponseWrapper
import com.forcetower.uefs.core.model.edge.paradox.EvaluationHotTopic
import com.forcetower.uefs.core.model.edge.paradox.EvaluationSnapshot
import com.forcetower.uefs.core.model.edge.paradox.PublicDisciplineEvaluationCombinedData
import com.forcetower.uefs.core.model.edge.paradox.PublicTeacherEvaluationCombinedData
import retrofit2.http.GET
import retrofit2.http.Query

interface ParadoxService {
    @GET("evaluation/all")
    suspend fun all(): ServiceResponseWrapper<EvaluationSnapshot>

    @GET("evaluation/hot")
    suspend fun hot(): ServiceResponseWrapper<List<EvaluationHotTopic>>

    @GET("evaluation/teacher")
    suspend fun teacher(@Query("id") id: String): ServiceResponseWrapper<PublicTeacherEvaluationCombinedData>

    @GET("evaluation/discipline")
    suspend fun discipline(@Query("id") id: String): ServiceResponseWrapper<PublicDisciplineEvaluationCombinedData>
}
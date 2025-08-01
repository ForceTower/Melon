package com.forcetower.uefs.core.storage.network

import com.forcetower.uefs.core.model.siecomp.ServerSession
import retrofit2.Call
import retrofit2.http.GET

interface BaseEdgeService {
    @GET("siecomp/sessions/sessions-2025.json")
    fun siecompSessions(): Call<List<ServerSession>>
}

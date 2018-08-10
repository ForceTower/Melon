package com.forcetower.unes.core.storage.network.adapter

import androidx.lifecycle.LiveData
import com.forcetower.unes.core.storage.resource.SagresResponse
import okhttp3.Call

fun Call.adapt(): LiveData<SagresResponse> = LiveDataCallAdapter.adapt(this)
package com.forcetower.unes.core.storage.network.adapter

import com.forcetower.unes.core.storage.resource.SagresResponse

import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean
import androidx.lifecycle.LiveData
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response

/**
 * A Retrofit adapter that converts the Call into a LiveData of ApiResponse.
 * @param <R>
</R> */
class LiveDataCallAdapter<R>(private val responseType: Type) : CallAdapter<R, LiveData<ApiResponse<R>>> {

    override fun responseType(): Type {
        return responseType
    }

    override fun adapt(call: Call<R>): LiveData<ApiResponse<R>> {
        return object : LiveData<ApiResponse<R>>() {
            var started = AtomicBoolean(false)
            override fun onActive() {
                super.onActive()
                if (started.compareAndSet(false, true)) {
                    call.enqueue(object : Callback<R> {
                        override fun onResponse(call: Call<R>, response: Response<R>) {
                            postValue(ApiResponse(response))
                        }

                        override fun onFailure(call: Call<R>, throwable: Throwable) {
                            postValue(ApiResponse(throwable))
                        }
                    })
                }
            }
        }
    }

    companion object {
        fun adapt(call: okhttp3.Call): LiveData<SagresResponse> {
            return object : LiveData<SagresResponse>() {
                var started = AtomicBoolean(false)
                override fun onActive() {
                    super.onActive()
                    if (started.compareAndSet(false, true)) {
                        call.enqueue(object : okhttp3.Callback {
                            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                                postValue(SagresResponse(response))
                            }

                            override fun onFailure(call: okhttp3.Call, throwable: IOException) {
                                postValue(SagresResponse(throwable))
                            }
                        })
                    }
                }
            }
        }
    }
}
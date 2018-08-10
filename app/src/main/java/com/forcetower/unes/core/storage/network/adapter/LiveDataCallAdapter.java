package com.forcetower.unes.core.storage.network.adapter;

import com.forcetower.unes.core.storage.resource.SagresResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A Retrofit adapter that converts the Call into a LiveData of ApiResponse.
 * @param <R>
 */
public class LiveDataCallAdapter<R> implements CallAdapter<R, LiveData<ApiResponse<R>>> {
    private final Type responseType;
    public LiveDataCallAdapter(Type responseType) {
        this.responseType = responseType;
    }

    @Override
    public Type responseType() {
        return responseType;
    }

    @Override
    public LiveData<ApiResponse<R>> adapt(@NonNull Call<R> call) {
        return new LiveData<ApiResponse<R>>() {
            AtomicBoolean started = new AtomicBoolean(false);
            @Override
            protected void onActive() {
                super.onActive();
                if (started.compareAndSet(false, true)) {
                    call.enqueue(new Callback<R>() {
                        @Override
                        public void onResponse(@NonNull Call<R> call, @NonNull Response<R> response) {
                            postValue(new ApiResponse<>(response));
                        }

                        @Override
                        public void onFailure(@NonNull Call<R> call, @NonNull Throwable throwable) {
                            postValue(new ApiResponse<>(throwable));
                        }
                    });
                }
            }
        };
    }

    public static LiveData<SagresResponse> adapt(@NonNull okhttp3.Call call) {
        return new LiveData<SagresResponse>() {
            AtomicBoolean started = new AtomicBoolean(false);
            @Override
            protected void onActive() {
                super.onActive();
                if (started.compareAndSet(false, true)) {
                    call.enqueue(new okhttp3.Callback() {
                        @Override
                        public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) {
                            postValue(new SagresResponse(response));
                        }

                        @Override
                        public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException throwable) {
                            postValue(new SagresResponse(throwable));
                        }
                    });
                }
            }
        };
    }
}
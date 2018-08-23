/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.core.storage.resource;

import com.forcetower.unes.core.storage.network.adapter.ActionError;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Resource<T> {
    @NonNull
    public final Status status;
    @Nullable
    public final String message;
    @Nullable
    public final T data;
    public final int code;
    @Nullable
    public final Throwable throwable;
    @Nullable
    public final ActionError actionError;

    public Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.code = -1;
        this.throwable = null;
        this.actionError = null;
    }

    public Resource(@NonNull Status status, @Nullable T data, @Nullable String message, int code) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.code = code;
        this.throwable = null;
        this.actionError = null;
    }

    public Resource(@NonNull Status status, @Nullable T data, @Nullable String message, int code, @Nullable Throwable throwable) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.code = code;
        this.throwable = throwable;
        this.actionError = null;
    }

    public Resource(@NonNull Status status, @Nullable T data, @Nullable String message, int code, @Nullable ActionError actionError) {
        this.status = status;
        this.data = data;
        this.message = message;
        this.code = code;
        this.throwable = null;
        this.actionError = actionError;
    }

    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    public static <T> Resource<T> error(String msg, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, msg);
    }

    public static <T> Resource<T> error(String msg, int code, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, msg, code);
    }

    public static <T> Resource<T> error(String msg, int code, Throwable throwable) {
        return new Resource<>(Status.ERROR, null, msg, code, throwable);
    }

    public static <T> Resource<T> error(String msg, int code, @Nullable ActionError actionError) {
        return new Resource<>(Status.ERROR, null, msg, code, actionError);
    }

    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Resource<?> resource = (Resource<?>) o;

        if (status != resource.status) {
            return false;
        }
        if (message != null ? !message.equals(resource.message) : resource.message != null) {
            return false;
        }
        return data != null ? data.equals(resource.data) : resource.data == null;
    }

    @Override
    public int hashCode() {
        int result = status.hashCode();
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}
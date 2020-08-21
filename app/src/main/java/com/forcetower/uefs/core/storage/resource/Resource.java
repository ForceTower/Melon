/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.core.storage.resource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.forcetower.uefs.core.storage.network.adapter.ActionError;

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

    public static <T> Resource<T> error(String msg, int code, Throwable throwable) {
        return new Resource<>(Status.ERROR, null, msg, code, throwable);
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
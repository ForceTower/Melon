/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
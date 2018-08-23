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

package com.forcetower.sagres.operation;
import org.jsoup.nodes.Document;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressWarnings("unchecked")
public abstract class BaseCallback<T extends BaseCallback> {
    @NonNull
    private final Status status;
    @Nullable
    private String message;
    private int code;
    @Nullable
    private Throwable throwable;
    @Nullable
    private Document document;

    public BaseCallback(@NonNull Status status) {
        this.status = status;
    }

    public final T message(@Nullable String message) {
        this.message = message;
        return (T)this;
    }

    public final T code(int code) {
        this.code = code;
        return (T)this;
    }

    public final T throwable(@Nullable Throwable throwable) {
        this.throwable = throwable;
        return (T)this;
    }

    public final T document(@Nullable Document document) {
        this.document = document;
        return (T)this;
    }

    @NonNull
    public final Status getStatus() {
        return status;
    }

    @Nullable
    public final String getMessage() {
        return message;
    }

    public final int getCode() {
        return code;
    }

    @Nullable
    public final Throwable getThrowable() {
        return throwable;
    }

    @Nullable
    public final Document getDocument() {
        return document;
    }
}

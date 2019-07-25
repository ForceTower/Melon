/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.sagres.operation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.jsoup.nodes.Document;

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

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

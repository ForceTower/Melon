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

public class Callback {
    @NonNull
    private final Status status;
    @Nullable
    private final String message;
    private final int code;
    @Nullable
    private final Throwable throwable;
    @Nullable
    private final Document document;

    protected Callback(@NonNull Status status, @Nullable String message, int code, @Nullable Throwable throwable, @Nullable Document document) {
        this.status = status;
        this.message = message;
        this.code = code;
        this.throwable = throwable;
        this.document = document;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    public Document getDocument() {
        return document;
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }

    public static class Builder {
        private final Status status;
        private String message = null;
        private int code = 200;
        private Throwable throwable = null;
        private Document document = null;

        public Builder(Status status) {
            this.status = status;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public Builder document(Document document) {
            this.document = document;
            return this;
        }

        public Callback build() {
            return new Callback(status, message, code, throwable, document);
        }
    }
}

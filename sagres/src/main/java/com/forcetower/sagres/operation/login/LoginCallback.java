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

package com.forcetower.sagres.operation.login;

import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;

import org.jsoup.nodes.Document;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LoginCallback extends BaseCallback<LoginCallback> {
    public LoginCallback(@NonNull Status status) {
        super(status);
    }

    static LoginCallback started() {
        return new Builder(Status.STARTED).build();
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

        public LoginCallback build() {
            return new LoginCallback(status).message(message).code(code).throwable(throwable).document(document);
        }
    }
}

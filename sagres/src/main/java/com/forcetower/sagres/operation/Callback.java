package com.forcetower.sagres.operation;

import org.jsoup.nodes.Document;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

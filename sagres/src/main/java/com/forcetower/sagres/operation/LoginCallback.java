package com.forcetower.sagres.operation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LoginCallback {
    @NonNull
    private final Status status;
    @Nullable
    private final String message;
    private final int code;
    @Nullable
    private final Throwable throwable;

    private LoginCallback(@NonNull Status status, @Nullable String message, int code, @Nullable Throwable throwable) {
        this.status = status;
        this.message = message;
        this.code = code;
        this.throwable = throwable;
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
    public Throwable getThrowable() {
        return throwable;
    }

    static LoginCallback started() {
        return new Builder(Status.STARTED).build();
    }

    public static class Builder {
        private final Status status;
        private String message = null;
        private int code = 200;
        private Throwable throwable = null;

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

        public LoginCallback build() {
            return new LoginCallback(status, message, code, throwable);
        }
    }
}

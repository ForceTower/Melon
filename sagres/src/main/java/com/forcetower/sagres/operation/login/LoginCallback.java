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

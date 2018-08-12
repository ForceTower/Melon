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

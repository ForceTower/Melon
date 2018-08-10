package com.forcetower.unes.core.storage.network.adapter;

/**
 * Created by João Paulo on 29/04/2018.
 */
public class ActionResult<T> {
    private String message;
    private T data;

    public ActionResult(String message, T data) {
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

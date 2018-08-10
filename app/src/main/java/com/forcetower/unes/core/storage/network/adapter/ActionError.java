package com.forcetower.unes.core.storage.network.adapter;

import java.util.HashMap;
import java.util.List;

/**
 * Created by João Paulo on 29/04/2018.
 */
public class ActionError {
    private int code;
    private String message;
    private boolean error;
    private HashMap<String, List<String>> errors;

    public ActionError(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public HashMap<String, List<String>> getErrors() {
        return errors;
    }

    public void setErrors(HashMap<String, List<String>> errors) {
        this.errors = errors;
    }
}

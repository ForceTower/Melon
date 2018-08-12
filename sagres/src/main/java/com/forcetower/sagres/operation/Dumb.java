package com.forcetower.sagres.operation;

import com.google.gson.annotations.SerializedName;

public class Dumb<T> {
    @SerializedName(value = "itens")
    private T items;

    public Dumb(T items) {
        this.items = items;
    }

    public T getItems() {
        return items;
    }

    public void setItems(T items) {
        this.items = items;
    }
}

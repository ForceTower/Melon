package com.forcetower.sagres.database.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SagresAccess {
    @PrimaryKey(autoGenerate = true)
    private long uid;
    @NonNull
    private String username;
    @NonNull
    private String password;

    public SagresAccess(@NonNull String username, @NonNull String password) {
        this.username = username;
        this.password = password;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public void setPassword(@NonNull String password) {
        this.password = password;
    }

    public boolean equals(Object other) {
        if (other instanceof SagresAccess) {
            SagresAccess created = (SagresAccess) other;
            return created.getPassword().equals(password) && created.getUsername().equals(username);
        }
        return false;
    }
}

package com.forcetower.sagres.database.model;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SagresAccess {
    @PrimaryKey(autoGenerate = true)
    private long uid;
    @NonNull
    private String uuid;
    @NonNull
    private String username;
    @NonNull
    private String password;

    public SagresAccess(@NonNull String username, @NonNull String password) {
        this.username = username;
        this.password = password;
        this.uuid = UUID.randomUUID().toString();
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    @NonNull
    public String getUuid() {
        return uuid;
    }

    public void setUuid(@NonNull String uuid) {
        this.uuid = uuid;
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
}

package com.forcetower.sagres.database;

import androidx.room.PrimaryKey;

public abstract class Identifiable {
    @PrimaryKey(autoGenerate = true)
    protected long uid;
    protected String uuid;

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}

/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.database.model;

import com.forcetower.sagres.database.Timestamped;
import com.google.gson.annotations.SerializedName;

import java.text.ParseException;

import androidx.annotation.NonNull;

public class SMessage implements Comparable<SMessage>, Timestamped {
    @SerializedName("id")
    private long sagresId;
    @SerializedName(value = "timeStamp")
    private String timestamp;
    @SerializedName(value = "descricao")
    private String message;
    @SerializedName(value = "perfilRemetente")
    private int senderProfile;
    private String senderName;
    @SerializedName(value = "remetente")
    private SLinker sender;

    public SMessage(long sagresId, String timestamp, SLinker sender, String message, int senderProfile, String senderName) {
        this.sagresId = sagresId;
        this.timestamp = timestamp;
        this.sender = sender;
        this.message = message;
        this.senderProfile = senderProfile;
        this.senderName = senderName;
    }

    public long getSagresId() {
        return sagresId;
    }

    public void setSagresId(long sagresId) {
        this.sagresId = sagresId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getSenderProfile() {
        return senderProfile;
    }

    public void setSenderProfile(int senderProfile) {
        this.senderProfile = senderProfile;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public SLinker getSender() {
        return sender;
    }

    public void setSender(SLinker sender) {
        this.sender = sender;
    }

    @Override
    public int compareTo(@NonNull SMessage o) {
        return Long.compare(getTimeStampInMillis(), o.getTimeStampInMillis());
    }

    public long getTimeStampInMillis() {
        try {
            return getInMillis(getTimestamp());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public String toString() {
        return getSenderName();
    }
}

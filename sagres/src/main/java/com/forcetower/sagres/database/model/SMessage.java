/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

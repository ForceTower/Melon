/*
 * Copyright (c) 2019.
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.forcetower.sagres.database.Timestamped;
import com.google.gson.annotations.SerializedName;

public class SMessage implements Comparable<SMessage>, Timestamped {
    @SerializedName("id")
    private long sagresId;
    @SerializedName(value = "timeStamp")
    private String timestamp;
    @SerializedName(value = "descricao")
    private String message;
    @SerializedName(value = "perfilRemetente")
    private int senderProfile;
    @Nullable
    private String senderName;
    @Nullable
    private String discipline;
    @SerializedName(value = "remetente")
    private SLinker sender;
    @SerializedName(value = "escopos")
    private SLinker scopes;
    @Nullable
    private String attachmentName;
    @Nullable
    private String attachmentLink;

    @Nullable
    private String disciplineCode;
    @Nullable
    private String objective;

    private boolean fromHtml;
    @Nullable
    private String dateString;
    private long processingTime;

    public SMessage(long sagresId, String timestamp, SLinker sender, String message, int senderProfile, @Nullable String senderName, SLinker scopes, @Nullable String attachmentName, @Nullable String attachmentLink) {
        this.sagresId = sagresId;
        this.timestamp = timestamp;
        this.sender = sender;
        this.message = message;
        this.senderProfile = senderProfile;
        this.senderName = senderName;
        this.scopes = scopes;
        this.attachmentLink = attachmentLink;
        this.attachmentName = attachmentName;
        this.fromHtml = false;
        this.processingTime = System.currentTimeMillis();
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

    @Nullable
    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(@Nullable String senderName) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public String toString() {
        String name = getSenderName();
        return (name == null ? "null" : name) +  "\n-> " + getMessage() + "\n\n";
    }

    public SLinker getScopes() {
        return scopes;
    }

    public void setScopes(SLinker scopes) {
        this.scopes = scopes;
    }

    @Nullable
    public String getDiscipline() {
        return discipline;
    }

    public void setDiscipline(@Nullable String discipline) {
        this.discipline = discipline;
    }

    @Nullable
    public String getDisciplineCode() {
        return disciplineCode;
    }

    public void setDisciplineCode(@Nullable String disciplineCode) {
        this.disciplineCode = disciplineCode;
    }

    @Nullable
    public String getObjective() {
        return objective;
    }

    public void setObjective(@Nullable String objective) {
        this.objective = objective;
    }

    public boolean isFromHtml() {
        return fromHtml;
    }

    public void setFromHtml(boolean fromHtml) {
        this.fromHtml = fromHtml;
    }

    @Nullable
    public String getDateString() {
        return dateString;
    }

    public void setDateString(@Nullable String dateString) {
        this.dateString = dateString;
    }

    public long getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(long processingTime) {
        this.processingTime = processingTime;
    }

    @Nullable
    public String getAttachmentLink() {
        return attachmentLink;
    }

    public void setAttachmentLink(@Nullable String attachmentLink) {
        this.attachmentLink = attachmentLink;
    }

    @Nullable
    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(@Nullable String attachmentName) {
        this.attachmentName = attachmentName;
    }
}

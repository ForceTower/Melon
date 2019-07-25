/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

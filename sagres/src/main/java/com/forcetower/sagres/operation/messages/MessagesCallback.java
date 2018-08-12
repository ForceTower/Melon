package com.forcetower.sagres.operation.messages;

import com.forcetower.sagres.database.model.Message;
import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MessagesCallback extends BaseCallback<MessagesCallback> {
    @Nullable private List<Message> messages;

    public MessagesCallback(@NonNull Status status) {
        super(status);
    }

    public MessagesCallback messages(List<Message> items) {
        this.messages = items;
        return this;
    }

    @Nullable
    public List<Message> getMessages() {
        return messages;
    }
}

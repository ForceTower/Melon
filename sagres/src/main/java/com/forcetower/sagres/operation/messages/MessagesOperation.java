package com.forcetower.sagres.operation.messages;

import com.forcetower.sagres.database.model.Linker;
import com.forcetower.sagres.database.model.Message;
import com.forcetower.sagres.database.model.Person;
import com.forcetower.sagres.operation.Dumb;
import com.forcetower.sagres.operation.Operation;
import com.forcetower.sagres.operation.Status;
import com.forcetower.sagres.request.SagresCalls;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Response;

public class MessagesOperation extends Operation<MessagesCallback> {
    private final long userId;

    public MessagesOperation(@Nullable Executor executor, long userId) {
        super(executor);
        this.userId = userId;
        this.perform();
    }

    @Override
    protected void execute() {
        result.postValue(new MessagesCallback(Status.STARTED));
        Call call = SagresCalls.getMessages(userId);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                successMeasures(body);
            } else {
                result.postValue(new MessagesCallback(Status.RESPONSE_FAILED).code(response.code()).message(response.message()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.postValue(new MessagesCallback(Status.NETWORK_ERROR).throwable(e));
        }
    }

    private void successMeasures(String body) {
        Type type = new TypeToken<Dumb<ArrayList<Message>>>(){}.getType();
        Dumb<List<Message>> dMessages = new Gson().fromJson(body, type);
        List<Message> items = dMessages.getItems();
        Collections.sort(items);
        for (Message message : items) {
            Person person = getPerson(message.getSender());
            if (person != null)
                message.setSenderName(person.getName());
            else
                System.out.println("Person is Invalid");
        }

        result.postValue(new MessagesCallback(Status.SUCCESS).messages(items));
    }

    private Person getPerson(Linker linker) {
        Call call = SagresCalls.getLink(linker);

        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                return gson.fromJson(body, Person.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

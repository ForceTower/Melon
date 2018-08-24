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

package com.forcetower.sagres.operation.messages;

import com.forcetower.sagres.database.model.SLinker;
import com.forcetower.sagres.database.model.SMessage;
import com.forcetower.sagres.database.model.SPerson;
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
        Type type = new TypeToken<Dumb<ArrayList<SMessage>>>(){}.getType();
        Dumb<List<SMessage>> dMessages = new Gson().fromJson(body, type);
        List<SMessage> items = dMessages.getItems();
        Collections.sort(items);
        for (SMessage message : items) {
            SPerson person = getPerson(message.getSender());
            if (person != null)
                message.setSenderName(person.getName());
            else
                System.out.println("SPerson is Invalid");
        }

        result.postValue(new MessagesCallback(Status.SUCCESS).messages(items));
    }

    private SPerson getPerson(SLinker linker) {
        Call call = SagresCalls.getLink(linker);

        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                return gson.fromJson(body, SPerson.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

package com.forcetower.sagres.operation.person;

import com.forcetower.sagres.database.model.Person;
import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Operation;
import com.forcetower.sagres.operation.Status;
import com.forcetower.sagres.request.SagresCalls;

import java.io.IOException;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Call;
import okhttp3.Response;

public class PersonOperation extends Operation<PersonCallback> {
    @Nullable private final Long userId;

    public PersonOperation(@Nullable Long userId, @Nullable Executor executor) {
        super(executor);
        this.userId = userId;
        this.perform();
    }

    @Override
    protected void execute() {
        result.postValue(new PersonCallback(Status.STARTED));
        Call call = userId == null ? SagresCalls.getMe() : SagresCalls.getPerson(userId);
        try {
            Response response = call.execute();
            if (response.isSuccessful()) {
                String body = response.body().string();
                Person user = gson.fromJson(body, Person.class);
                successMeasures(user);
            } else {
                result.postValue(new PersonCallback(Status.RESPONSE_FAILED).code(response.code()).message(response.message()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.postValue(new PersonCallback(Status.NETWORK_ERROR).throwable(e));
        }
    }

    private void successMeasures(@NonNull Person user) {
        result.postValue(new PersonCallback(Status.SUCCESS).person(user));
    }
}

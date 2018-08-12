package com.forcetower.sagres.operation.person;

import com.forcetower.sagres.database.model.Person;
import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PersonCallback extends BaseCallback<PersonCallback> {
    @Nullable
    private Person person;

    PersonCallback(@NonNull Status status) {
        super(status);
    }


    public PersonCallback person(@Nullable Person person) {
        this.person = person;
        return this;
    }

    @Nullable
    public Person getPerson() {
        return person;
    }
}

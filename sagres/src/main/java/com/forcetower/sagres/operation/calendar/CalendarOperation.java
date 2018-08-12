package com.forcetower.sagres.operation.calendar;

import com.forcetower.sagres.operation.Operation;
import com.forcetower.sagres.operation.Status;

import java.util.concurrent.Executor;

import androidx.annotation.Nullable;

public class CalendarOperation extends Operation<CalendarCallback> {

    public CalendarOperation(@Nullable Executor executor) {
        super(executor);
        this.perform();
    }

    @Override
    protected void execute() {
        result.postValue(new CalendarCallback(Status.STARTED));
    }
}

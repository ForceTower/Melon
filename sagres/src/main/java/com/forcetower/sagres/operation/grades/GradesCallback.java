package com.forcetower.sagres.operation.grades;

import com.forcetower.sagres.operation.BaseCallback;
import com.forcetower.sagres.operation.Status;

import androidx.annotation.NonNull;

public class GradesCallback extends BaseCallback<GradesCallback> {

    public GradesCallback(@NonNull Status status) {
        super(status);
    }
}

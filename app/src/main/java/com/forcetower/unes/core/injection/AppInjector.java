package com.forcetower.unes.core.injection;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.forcetower.unes.UApplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.android.support.HasSupportFragmentInjector;

public final class AppInjector {

    public static AndroidInjector<UApplication> create(UApplication application) {
        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacksAdapter() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                handleActivity(activity);
            }
        });
        return DaggerAppComponent.builder().create(application);
    }

    private static void handleActivity(Activity activity) {
        if (activity instanceof HasSupportFragmentInjector) {
            AndroidInjection.inject(activity);

            if (activity instanceof FragmentActivity) {
                ((FragmentActivity) activity).getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
                        super.onFragmentCreated(fm, f, savedInstanceState);
                        if (f instanceof Injectable)
                            AndroidSupportInjection.inject(f);
                    }
                }, true);
            }
        }
    }
}

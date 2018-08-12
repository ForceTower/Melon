package com.forcetower.unes.core.injection

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.forcetower.unes.UApplication
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector

object AppInjection {
    fun create(application: UApplication): AndroidInjector<UApplication> {
        application.registerActivityLifecycleCallbacks(object: ActLifecycleCbAdapter() {
            override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                handle(activity)
            }
        })
        return DaggerAppComponent.builder().create(application)
    }

    private fun handle(activity: Activity?) {
        if (activity is HasSupportFragmentInjector) {
            AndroidInjection.inject(activity)
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(object: FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentCreated(fm: FragmentManager, f: Fragment, savedInstanceState: Bundle?) {
                        if (f is Injectable) AndroidSupportInjection.inject(f)
                    }
                }, true)
            }
        }
    }
}
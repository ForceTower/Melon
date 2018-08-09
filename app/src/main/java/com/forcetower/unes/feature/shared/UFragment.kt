package com.forcetower.unes.feature.shared

import android.content.Context
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import timber.log.Timber

abstract class UFragment : Fragment() {

    fun showSnack(string: String) {
        val activity = activity
        if (activity is UActivity) {
            activity.showSnack(string)
        } else {
            Timber.d("Not part of UActivity")
        }
    }

    fun getToolbar(): Toolbar = (activity!! as ToolbarActivity).getToolbar()
}
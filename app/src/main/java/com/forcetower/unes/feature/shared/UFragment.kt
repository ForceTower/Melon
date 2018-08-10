package com.forcetower.unes.feature.shared

import android.content.Context
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout
import timber.log.Timber

abstract class UFragment : Fragment() {
    var displayName: String = javaClass.simpleName

    fun showSnack(string: String) {
        val activity = activity
        if (activity is UActivity) {
            activity.showSnack(string)
        } else {
            Timber.d("Not part of UActivity")
        }
    }

    fun getToolbar(): Toolbar = (activity as ToolbarActivity).getToolbar()
    fun getAppBar(): AppBarLayout = (activity as ToolbarActivity).getAppBar()
    fun getTabLayout(): TabLayout = (activity as ToolbarActivity).getTabLayout()
}
package com.forcetower.unes.feature.shared

import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout

interface ToolbarActivity {
    fun getToolbar(): Toolbar
    fun getTabLayout(): TabLayout
    fun getAppBar(): AppBarLayout
}
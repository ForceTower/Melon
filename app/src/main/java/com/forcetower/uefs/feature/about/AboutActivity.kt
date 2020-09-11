/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.about

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.ActivityAboutBinding
import com.forcetower.uefs.feature.shared.FragmentAdapter
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.themeswitcher.ThemeOverlayUtils
import com.forcetower.uefs.widget.ElasticDragDismissFrameLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutActivity : UActivity() {
    private lateinit var binding: ActivityAboutBinding
    private val adapter: FragmentAdapter by lazy { FragmentAdapter(supportFragmentManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeOverlayUtils.applyThemeOverlays(this, intArrayOf(R.id.theme_feature_background_color))
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityAboutBinding>(this, R.layout.activity_about).also {
            binding = it
        }

        binding.draggableFrame.addListener(
            object : ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
                override fun onDragDismissed() {
                    if (binding.draggableFrame.translationY > 0) {
                        window.returnTransition = TransitionInflater.from(this@AboutActivity)
                            .inflateTransition(R.transition.about_return_downward)
                    }
                    finishAfterTransition()
                }
            }
        )

        setupPager()
    }

    private fun setupPager() {
        val about = AboutMeFragment()
        val contributors = ContributorsFragment()
        adapter.setItems(listOf(about, contributors))

        binding.viewPager.adapter = adapter
        binding.indicator.setViewPager(binding.viewPager)
    }

    override fun shouldApplyThemeOverlay() = false

    companion object {
        fun startActivity(activity: Activity) {
            val intent = Intent(activity, AboutActivity::class.java)
            val bundle = ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()
            activity.startActivity(intent, bundle)
        }
    }
}

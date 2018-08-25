/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.feature.about

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.forcetower.unes.R
import com.forcetower.unes.databinding.ActivityAboutBinding
import com.forcetower.unes.feature.shared.FragmentAdapter
import com.forcetower.unes.feature.shared.UActivity
import com.forcetower.unes.widget.ElasticDragDismissFrameLayout
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject


class AboutActivity : UActivity(), HasSupportFragmentInjector {
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    private lateinit var binding: ActivityAboutBinding
    private val adapter: FragmentAdapter by lazy { FragmentAdapter(supportFragmentManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityAboutBinding>(this, R.layout.activity_about).also {
            binding = it
        }

        binding.draggableFrame.addListener(object: ElasticDragDismissFrameLayout.ElasticDragDismissCallback() {
            override fun onDragDismissed() {
                if (binding.draggableFrame.translationY > 0) {
                    window.returnTransition = TransitionInflater.from(this@AboutActivity)
                            .inflateTransition(R.transition.about_return_downward)
                }
                finishAfterTransition()
            }
        })

        setupPager()
    }

    private fun setupPager() {
        val about = AboutMeFragment()
        val contributors = ContributorsFragment()
        adapter.setItems(listOf(about, contributors))

        binding.viewPager.adapter = adapter
        binding.indicator.setViewPager(binding.viewPager)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    companion object {
        fun startActivity(activity: Activity) {
            val intent = Intent(activity, AboutActivity::class.java)
            val bundle = ActivityOptions.makeSceneTransitionAnimation(activity).toBundle()
            activity.startActivity(intent, bundle)
        }
    }
}

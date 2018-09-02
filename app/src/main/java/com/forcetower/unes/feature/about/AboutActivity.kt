/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.model.unes.Contributor
import com.forcetower.unes.core.util.MockUtils
import com.forcetower.unes.databinding.FragmentAboutContributorsBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.web.CustomTabActivityHelper

class ContributorsFragment: UFragment(), Injectable {
    private lateinit var binding: FragmentAboutContributorsBinding
    private val adapter: ContributorAdapter by lazy { ContributorAdapter(listener = object: ContributorActions {
        override fun onContributorClick(contributor: Contributor, position: Int) {
            if (contributor.link.isNotBlank()) {
                CustomTabActivityHelper.openCustomTab(
                        requireActivity(),
                        CustomTabsIntent.Builder()
                                .setToolbarColor(ContextCompat.getColor(requireContext(), R.color.blue_accent))
                                .addDefaultShareMenuItem()
                                .build(),
                        Uri.parse(contributor.link))
            }
        }
    }) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentAboutContributorsBinding.inflate(inflater, container, false).also {
            binding = it
        }
        setupRecyclerView()
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.recyclerContributors.adapter = adapter
        binding.recyclerContributors.itemAnimator = DefaultItemAnimator()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter.submitList(MockUtils.contributors())
    }
}

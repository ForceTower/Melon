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
import com.forcetower.unes.core.model.Contributor
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

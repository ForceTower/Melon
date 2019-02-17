/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.about

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Contributor
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentAboutContributorsBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import com.forcetower.uefs.feature.web.CustomTabActivityHelper
import timber.log.Timber
import javax.inject.Inject

class ContributorsFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: ContributorViewModel

    private lateinit var binding: FragmentAboutContributorsBinding
    private val adapter: ContributorAdapter by lazy { ContributorAdapter(viewModel) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        FragmentAboutContributorsBinding.inflate(inflater, container, false).also {
            binding = it
        }
        setupRecyclerView()
        return binding.root
    }

    private fun setupRecyclerView() {
        binding.recyclerContributors.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.contributorClickAction.observe(this, EventObserver { openLink(it) })
        viewModel.contributors.observe(this, Observer { processContributors(it) })
    }

    private fun processContributors(resource: Resource<List<Contributor>>?) {
        when (resource?.status) {
            Status.ERROR -> Timber.d("Failed to load contributors")
            Status.LOADING -> {
                Timber.d("Still loading")
                populateInterface(resource.data)
            }
            Status.SUCCESS -> {
                Timber.d("Loaded!")
                populateInterface(resource.data)
            }
        }
    }

    private fun populateInterface(data: List<Contributor>?) {
        data ?: return
        adapter.submitList(data)
    }

    private fun openLink(string: String) {
        CustomTabActivityHelper.openCustomTab(
            requireActivity(),
            CustomTabsIntent.Builder()
                .setToolbarColor(ContextCompat.getColor(requireContext(), R.color.blue_accent))
                .addDefaultShareMenuItem()
                .build(),
            Uri.parse(string))
    }
}

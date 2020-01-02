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

package com.forcetower.uefs.aeri.feature

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.forcetower.core.base.BaseViewModelFactory
import com.forcetower.uefs.R
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.aeri.core.injection.DaggerAERIComponent
import com.forcetower.uefs.aeri.databinding.FragmentAeriNewsBinding
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.web.CustomTabActivityHelper
import timber.log.Timber
import javax.inject.Inject

class AERINewsFragment : UFragment() {
    @Inject
    lateinit var factory: BaseViewModelFactory
    private lateinit var binding: FragmentAeriNewsBinding
    private val viewModel: AERIViewModel by activityViewModels { factory }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val component = (context.applicationContext as UApplication).component
        DaggerAERIComponent.builder().appComponent(component).build().inject(this)
        Timber.d("Factory initialized? ${::factory.isInitialized}")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentAeriNewsBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            messagesViewModel = viewModel
            lifecycleOwner = this@AERINewsFragment
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = AERIMessagesAdapter(viewModel)
        binding.apply {
            recyclerNews.adapter = adapter
            recyclerNews.itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }

        viewModel.announcements.observe(viewLifecycleOwner, Observer { adapter.submitList(it) })
        viewModel.announcementClick.observe(viewLifecycleOwner, EventObserver {
            CustomTabActivityHelper.openCustomTab(
                requireActivity(),
                CustomTabsIntent.Builder()
                    .setToolbarColor(ContextCompat.getColor(requireContext(), R.color.blue_accent))
                    .addDefaultShareMenuItem()
                    .build(),
                Uri.parse(it.link))
        })
    }
}
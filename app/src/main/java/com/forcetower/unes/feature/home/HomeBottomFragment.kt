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

package com.forcetower.unes.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.vm.HomeViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.HomeBottomBinding
import com.forcetower.unes.feature.about.AboutActivity
import com.forcetower.unes.feature.shared.RoundedBottomSheetDialogFragment
import com.forcetower.unes.feature.shared.provideViewModel
import kotlinx.android.synthetic.main.fragment_home_bottom_sheet.*
import timber.log.Timber
import javax.inject.Inject

class HomeBottomFragment: RoundedBottomSheetDialogFragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: UViewModelFactory

    private lateinit var binding: HomeBottomBinding
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(viewModelFactory)
        return HomeBottomBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            setLifecycleOwner(this@HomeBottomFragment)
            viewModel = this@HomeBottomFragment.viewModel
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        navigation_view.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.messages -> activity?.findNavController(R.id.home_nav_host)?.navigate(R.id.messages)
                R.id.grades_disciplines -> Timber.d("Grades")
                R.id.about -> AboutActivity.startActivity(requireActivity())
            }
            dismiss()
            true
        }
    }
}
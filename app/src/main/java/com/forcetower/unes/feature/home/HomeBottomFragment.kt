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

package com.forcetower.unes.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.vm.HomeViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.HomeBottomBinding
import com.forcetower.unes.feature.about.AboutActivity
import com.forcetower.unes.feature.shared.RoundedBottomSheetDialogFragment
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.provideActivityViewModel
import com.forcetower.unes.feature.shared.provideViewModel
import kotlinx.android.synthetic.main.fragment_home_bottom_sheet.*
import timber.log.Timber
import javax.inject.Inject

class HomeBottomFragment: UFragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: UViewModelFactory

    private lateinit var binding: HomeBottomBinding
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(viewModelFactory)
        getToolbarTitleText().text = getString(R.string.label_option_menu)
        getTabLayout().visibility = GONE
        getAppBar().elevation = 0f

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
        NavigationUI.setupWithNavController(binding.navigationView, findNavController())
    }
}
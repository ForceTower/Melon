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

package com.forcetower.uefs.feature.bigtray

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.architecture.service.bigtray.BigTrayService
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.bigtray.BigTrayData
import com.forcetower.uefs.core.model.bigtray.isOpen
import com.forcetower.uefs.core.model.bigtray.percentage
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentBigTrayBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.formatDateTime
import com.forcetower.uefs.feature.shared.provideViewModel
import javax.inject.Inject

class BigTrayFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: BigTrayViewModel
    private lateinit var binding: FragmentBigTrayBinding
    private var hasData = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
        return FragmentBigTrayBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.incToolbar.apply {
            textToolbarTitle.text = getString(R.string.label_big_tray)
            appBar.elevation = 0f
        }
        binding.btnNotification.setOnClickListener { BigTrayService.startService(context!!) }
    }

    override fun onStart() {
        super.onStart()
        viewModel.requesting = true
        viewModel.data().observe(this, Observer { onDataSnapshot(it) })
    }

    private fun onDataSnapshot(data: BigTrayData?) {
        data?: return
        binding.groupLoading.visibility = GONE
        binding.textUpdate.text = getString(R.string.ru_last_update, data.time.formatDateTime())

        if (data.isOpen() && !data.error) {
            binding.groupOpen.visibility = VISIBLE
            binding.groupClosed.visibility = GONE
            binding.groupFailed.visibility = GONE
            val percent = data.percentage()
            binding.progressAmount.setProgressWithAnimation(percent)
            binding.textAmount.text = data.quota
            hasData = true
        } else if (!data.error) {
            binding.groupOpen.visibility = GONE
            binding.groupClosed.visibility = VISIBLE
            binding.groupFailed.visibility = GONE
            hasData = true
        } else if (!hasData){
            binding.groupOpen.visibility = GONE
            binding.groupClosed.visibility = GONE
            binding.groupFailed.visibility = VISIBLE
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.requesting = false
    }

}

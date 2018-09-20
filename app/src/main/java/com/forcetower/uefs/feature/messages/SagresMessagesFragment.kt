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

package com.forcetower.uefs.feature.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.vm.HomeViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentSagresMessagesBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.forcetower.uefs.feature.shared.provideViewModel
import javax.inject.Inject

class SagresMessagesFragment: UFragment(), Injectable {
    @Inject
    lateinit var vmFactory: UViewModelFactory

    init { displayName = "Sagres" }

    private val adapter by lazy { SagresMessageAdapter()}
    private val manager by lazy { LinearLayoutManager(context) }
    private lateinit var binding: FragmentSagresMessagesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSagresMessagesBinding.inflate(inflater, container, false)
        setupRecycler()
        return binding.root
    }

    private fun setupRecycler() {
        binding.apply {
            recyclerSagresMessages.adapter = adapter
            recyclerSagresMessages.layoutManager = manager
            recyclerSagresMessages.itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        provideViewModel<HomeViewModel>(vmFactory).messages.observe(this, Observer { onMessagesChange(it) })
    }

    private fun onMessagesChange(list: List<Message>) {
        adapter.submitList(list)
    }
}
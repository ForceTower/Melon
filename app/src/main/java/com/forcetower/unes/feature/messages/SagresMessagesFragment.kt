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

package com.forcetower.unes.feature.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.model.unes.Message
import com.forcetower.unes.core.vm.HomeViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentSagresMessagesBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.getPixelsFromDp
import com.forcetower.unes.feature.shared.provideViewModel
import timber.log.Timber
import javax.inject.Inject

class SagresMessagesFragment: UFragment(), Injectable {
    @Inject
    lateinit var vmFactory: UViewModelFactory

    init { displayName = "Sagres" }

    private val adapter by lazy { SagresMessageAdapter(diffCallback = object: DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.sagresId == newItem.sagresId
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
    })}

    private val manager by lazy { LinearLayoutManager(context) }

    private lateinit var binding: FragmentSagresMessagesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSagresMessagesBinding.inflate(inflater, container, false)
        setupRecycler()
        return binding.root
    }

    private fun setupRecycler() {
        binding.recyclerSagresMessages.adapter = adapter
        binding.recyclerSagresMessages.layoutManager = manager
        binding.recyclerSagresMessages.itemAnimator = DefaultItemAnimator()
        binding.recyclerSagresMessages.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (manager.findFirstCompletelyVisibleItemPosition() == 0) getAppBar().elevation = 0F
                else getAppBar().elevation = getPixelsFromDp(requireContext(), 6).toFloat()
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        provideViewModel<HomeViewModel>(vmFactory).messages.observe(this, Observer { onMessagesChange(it) })
    }

    private fun onMessagesChange(list: PagedList<Message>) {
        Timber.d("Messages List size is ${list.size}")
        adapter.submitList(list)
    }
}
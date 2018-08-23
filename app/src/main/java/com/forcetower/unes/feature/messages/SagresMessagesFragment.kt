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
import com.forcetower.unes.R
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.core.vm.HomeViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentSagresMessagesBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.getPixelsFromDp
import com.forcetower.unes.feature.shared.provideViewModel
import kotlinx.android.synthetic.main.fragment_unes_messages.*
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
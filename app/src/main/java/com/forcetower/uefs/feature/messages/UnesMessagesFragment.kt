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

package com.forcetower.uefs.feature.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.databinding.FragmentUnesMessagesBinding
import com.forcetower.uefs.feature.shared.UFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UnesMessagesFragment : UFragment() {
    init { displayName = "UNES" }

    private lateinit var binding: FragmentUnesMessagesBinding

    private val viewModel: MessagesViewModel by activityViewModels()
    private lateinit var messagesAdapter: UnesMessageAdapter
    private lateinit var adapterDataObserver: RecyclerView.AdapterDataObserver

    private var initialized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentUnesMessagesBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        messagesAdapter = UnesMessageAdapter(this, viewModel)
        binding.recyclerSagresMessages.apply {
            adapter = messagesAdapter
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }

        val manager = binding.recyclerSagresMessages.layoutManager as LinearLayoutManager
        adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0 && initialized) {
                    manager.smoothScrollToPosition(binding.recyclerSagresMessages, null, 0)
                }
                initialized = true
            }
        }

        messagesAdapter.registerAdapterDataObserver(adapterDataObserver)
        viewModel.unesMessages.observe(viewLifecycleOwner, Observer { messagesAdapter.submitList(it) })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // safe clean-up (prob unnecessary)
        if (::adapterDataObserver.isInitialized) {
            messagesAdapter.unregisterAdapterDataObserver(adapterDataObserver)
        }
    }
}

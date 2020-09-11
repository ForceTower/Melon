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

package com.forcetower.uefs.feature.reminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.FragmentRemindersBinding
import com.forcetower.uefs.feature.shared.SwipeDeleteHandler
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RemindersFragment : UFragment() {
    private val viewModel: RemindersViewModel by viewModels()
    private lateinit var binding: FragmentRemindersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentRemindersBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            binding.incToolbar.textToolbarTitle.text = getString(R.string.label_reminders)
            binding.fab.setOnClickListener { openAddReminder() }
        }.root
    }

    private fun openAddReminder() {
        val create = CreateReminderDialog()
        create.show(childFragmentManager, "create_reminder")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val reminderAdapter = RemindersAdapter(this, viewModel)
        binding.recyclerReminders.apply {
            adapter = reminderAdapter
            itemAnimator?.apply {
                addDuration = 220L
                moveDuration = 220L
                changeDuration = 220L
                removeDuration = 200L
            }
        }

        binding.recyclerReminders.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val manager = binding.recyclerReminders.layoutManager as? LinearLayoutManager
                    if (manager != null) {
                        if (manager.findFirstCompletelyVisibleItemPosition() != 0) {
                            binding.incToolbar.appBar.elevation = getPixelsFromDp(requireContext(), 4)
                        } else {
                            binding.incToolbar.appBar.elevation = getPixelsFromDp(requireContext(), 0)
                        }
                    }
                }
            }
        )

        val helper = ItemTouchHelper(
            SwipeDeleteHandler(
                requireContext(),
                {
                    val holder = it as? ReminderHolder.ItemHolder
                    if (holder != null) {
                        val reminder = holder.binding.reminder
                        if (reminder != null) viewModel.deleteReminder(reminder)
                    }
                },
                ignored = listOf(ReminderHolder.CompletedHeaderHolder::class.java)
            )
        )
        helper.attachToRecyclerView(binding.recyclerReminders)

        viewModel.reminders.observe(
            viewLifecycleOwner,
            Observer {
                reminderAdapter.currentReminders = it
                if (it.isEmpty()) {
                    binding.recyclerReminders.visibility = View.GONE
                    binding.layoutNoData.visibility = View.VISIBLE
                } else {
                    binding.recyclerReminders.visibility = View.VISIBLE
                    binding.layoutNoData.visibility = View.GONE
                }
            }
        )
    }
}

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

package com.forcetower.uefs.feature.reminders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentRemindersBinding
import com.forcetower.uefs.feature.shared.SwipeDeleteHandler
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.forcetower.uefs.feature.shared.provideViewModel
import javax.inject.Inject

class RemindersFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var viewModel: RemindersViewModel
    private lateinit var binding: FragmentRemindersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideViewModel(factory)
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

        binding.recyclerReminders.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        })

        val helper = ItemTouchHelper(SwipeDeleteHandler(requireContext(), {
            val holder = it as? ReminderHolder.ItemHolder
            if (holder != null) {
                val reminder = holder.binding.reminder
                if (reminder != null) viewModel.deleteReminder(reminder)
            }
        }, ignored = listOf(ReminderHolder.CompletedHeaderHolder::class.java)))
        helper.attachToRecyclerView(binding.recyclerReminders)

        viewModel.reminders.observe(this, Observer { reminderAdapter.currentReminders = it })
    }
}
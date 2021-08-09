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

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.UApplication
import com.forcetower.uefs.core.storage.database.UDatabase
import com.forcetower.uefs.core.util.getLinks
import com.forcetower.uefs.core.util.isStudentFromUEFS
import com.forcetower.uefs.databinding.FragmentAllMessagesBinding
import com.forcetower.uefs.feature.home.HomeViewModel
import com.forcetower.uefs.feature.messages.dynamic.AERIMessageFragment
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.openURL
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MessagesFragment : UFragment() {
    @Inject
    lateinit var preferences: SharedPreferences
    @Inject
    lateinit var database: UDatabase

    private lateinit var binding: FragmentAllMessagesBinding
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val messagesViewModel: MessagesViewModel by activityViewModels()
    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAllMessagesBinding.inflate(inflater, container, false).apply {
            profileViewModel = this@MessagesFragment.profileViewModel
            lifecycleOwner = this@MessagesFragment
        }

        preparePager()
        return binding.root
    }

    private fun preparePager() {
        val tabLayout = binding.tabLayout
        tabLayout.visibility = VISIBLE

        tabLayout.clearOnTabSelectedListeners()
        tabLayout.removeAllTabs()

        tabLayout.setupWithViewPager(binding.pagerMessage)
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(binding.pagerMessage))
        binding.pagerMessage.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        val fragments = mutableListOf<UFragment>()
        fragments += SagresMessagesFragment()
        fragments += UnesMessagesFragment()
        if (preferences.isStudentFromUEFS() && preferences.getBoolean("stg_advanced_aeri_tab", true)) {
            fragments += AERIMessageFragment()
        }

        binding.pagerMessage.adapter = SectionFragmentAdapter(childFragmentManager, fragments)
        binding.textToolbarTitle.setOnLongClickListener {
            lifecycleScope.launch {
                database.messageDao().deleteAllSuspend()
            }
            true
        }
        binding.textToolbarTitle.setOnClickListener {
            (requireContext().applicationContext as UApplication).messageToolbarDevClickCount++
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val index = arguments?.getInt(EXTRA_MESSAGES_FLAG, 0) ?: 0
        val open = savedInstanceState?.getBoolean(EXTRA_OPEN_MESSAGES_FLAG, true) ?: true
        if (index > 0 && open) {
            binding.pagerMessage.setCurrentItem(index, true)
        }

        messagesViewModel.messageClick.observe(viewLifecycleOwner, EventObserver { openLink(it) })
        messagesViewModel.snackMessage.observe(viewLifecycleOwner, EventObserver { showSnack(getString(it), Snackbar.LENGTH_LONG) })
    }

    private fun openLink(content: String) {
        val links = content.getLinks()
        if (links.isEmpty()) return

        if (links.size == 1) {
            try {
                requireContext().openURL(links[0])
            } catch (ignored: Throwable) {
                homeViewModel.showSnack(getString(R.string.unable_to_open_url))
            }
        } else {
            val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.select_dialog_item)
            adapter.addAll(links)

            val dialog = AlertDialog.Builder(requireContext())
                .setIcon(R.drawable.ic_http_accent_30dp)
                .setTitle(R.string.select_a_link)
                .setAdapter(adapter) { dialog, position ->
                    val url = adapter.getItem(position)
                    dialog.dismiss()
                    try {
                        if (url != null) requireContext().openURL(url)
                    } catch (ignored: Throwable) {
                        homeViewModel.showSnack(getString(R.string.unable_to_open_url))
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .create()

            dialog.show()
        }
    }

    override fun onStop() {
        super.onStop()
        messagesViewModel.pushedTimes = 0
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(EXTRA_OPEN_MESSAGES_FLAG, false)
        super.onSaveInstanceState(outState)
    }

    private class SectionFragmentAdapter(fm: FragmentManager, val fragments: List<UFragment>) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount() = fragments.size
        override fun getItem(position: Int) = fragments[position]
        override fun getPageTitle(position: Int) = fragments[position].displayName
    }

    companion object {
        const val EXTRA_MESSAGES_FLAG = "unes.messages.is_svc_message"
        const val EXTRA_OPEN_MESSAGES_FLAG = "unes.messages.opened_notification"

        fun newInstance(fragmentIndex: Int): MessagesFragment {
            return MessagesFragment().apply {
                arguments = bundleOf(EXTRA_MESSAGES_FLAG to fragmentIndex)
            }
        }
    }
}

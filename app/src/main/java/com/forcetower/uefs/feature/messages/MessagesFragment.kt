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
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.util.getLinks
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentAllMessagesBinding
import com.forcetower.uefs.feature.home.HomeViewModel
import com.forcetower.uefs.feature.profile.ProfileViewModel
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.openURL
import com.forcetower.uefs.feature.shared.extensions.provideActivityViewModel
import com.google.android.material.tabs.TabLayout
import java.util.Arrays
import javax.inject.Inject

class MessagesFragment : UFragment(), Injectable {
    companion object {
        const val EXTRA_MESSAGES_FLAG = "unes.messages.is_svc_message"

        fun newInstance(unesService: Boolean): MessagesFragment {
            return MessagesFragment().apply {
                arguments = bundleOf(EXTRA_MESSAGES_FLAG to unesService)
            }
        }
    }

    @Inject
    lateinit var factory: UViewModelFactory

    private lateinit var binding: FragmentAllMessagesBinding
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var messagesViewModel: MessagesViewModel
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        profileViewModel = provideActivityViewModel(factory)
        messagesViewModel = provideActivityViewModel(factory)
        homeViewModel = provideActivityViewModel(factory)

        binding = FragmentAllMessagesBinding.inflate(inflater, container, false).apply {
            profileViewModel = this@MessagesFragment.profileViewModel
            messagesViewModel = this@MessagesFragment.messagesViewModel
            setLifecycleOwner(this@MessagesFragment)
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

        val sagres = SagresMessagesFragment()
        val unes = UnesMessagesFragment()

        binding.pagerMessage.adapter = SectionFragmentAdapter(childFragmentManager, Arrays.asList(sagres, unes))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val unes = arguments?.getBoolean(EXTRA_MESSAGES_FLAG, false) ?: false
        val open = savedInstanceState?.getBoolean(EXTRA_MESSAGES_FLAG, true) ?: true
        if (unes && open) {
            binding.pagerMessage.setCurrentItem(1, true)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        messagesViewModel.messageClick.observe(this, EventObserver { openLink(it) })
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(EXTRA_MESSAGES_FLAG, false)
        super.onSaveInstanceState(outState)
    }

    private class SectionFragmentAdapter(fm: FragmentManager, val fragments: List<UFragment>) : FragmentPagerAdapter(fm) {
        override fun getCount() = fragments.size
        override fun getItem(position: Int) = fragments[position]
        override fun getPageTitle(position: Int) = fragments[position].displayName
    }
}
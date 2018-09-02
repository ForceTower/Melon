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
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.forcetower.unes.R
import com.forcetower.unes.databinding.FragmentAllMessagesBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.fadeIn
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_all_messages.*
import java.util.*

class MessagesFragment: UFragment() {
    private lateinit var binding: FragmentAllMessagesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentAllMessagesBinding.inflate(inflater, container, false)
        getToolbarTitleText().text = getString(R.string.label_messages)
        preparePager()
        return binding.root
    }

    private fun preparePager() {
        val tabLayout = getTabLayout()
        tabLayout.visibility = VISIBLE

        tabLayout.clearOnTabSelectedListeners()
        tabLayout.removeAllTabs()

        tabLayout.setupWithViewPager(binding.pagerMessage)
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(binding.pagerMessage))
        binding.pagerMessage.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        val sagres = SagresMessagesFragment()
        val unes   = UnesMessagesFragment()

        binding.pagerMessage.adapter = SectionFragmentAdapter(childFragmentManager, Arrays.asList(sagres, unes))
    }

    private class SectionFragmentAdapter(fm: FragmentManager, val fragments: List<UFragment>): FragmentPagerAdapter(fm) {
        override fun getCount(): Int = fragments.size
        override fun getItem(position: Int): Fragment = fragments[position]
        override fun getPageTitle(position: Int): CharSequence? = fragments[position].displayName
    }
}
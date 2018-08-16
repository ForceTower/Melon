package com.forcetower.unes.feature.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
        tabLayout.fadeIn()

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
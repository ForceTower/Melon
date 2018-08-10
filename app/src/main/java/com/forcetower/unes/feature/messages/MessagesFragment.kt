package com.forcetower.unes.feature.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.forcetower.unes.R
import com.forcetower.unes.feature.shared.UFragment
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.fragment_all_messages.*
import java.util.*

class MessagesFragment: UFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_all_messages, container, false)
        preparePager()
        return view
    }

    private fun preparePager() {
        val tabLayout = getTabLayout()

        tabLayout.clearOnTabSelectedListeners()
        tabLayout.removeAllTabs()

        tabLayout.setupWithViewPager(pager_message)
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(pager_message))
        pager_message.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        val sagres = SagresMessagesFragment()
        val unes   = UnesMessagesFragment()

        pager_message.adapter = SectionFragmentAdapter(childFragmentManager, Arrays.asList(sagres, unes))
    }

    private class SectionFragmentAdapter(fm: FragmentManager, val fragments: List<UFragment>): FragmentPagerAdapter(fm) {
        override fun getCount(): Int = fragments.size
        override fun getItem(position: Int): Fragment = fragments[position]
        override fun getPageTitle(position: Int): CharSequence? = fragments[position].displayName
    }
}
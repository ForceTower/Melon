package com.forcetower.uefs.feature.disciplines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.vm.DisciplineViewModel
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentDisciplineBinding
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.makeSemester
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import com.google.android.material.tabs.TabLayout
import javax.inject.Inject

class DisciplineFragment: UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: DisciplineViewModel
    private lateinit var binding: FragmentDisciplineBinding

    private lateinit var viewPager: ViewPager
    private lateinit var tabs: TabLayout
    private lateinit var adapter: SemesterAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        return FragmentDisciplineBinding.inflate(inflater, container, false).also {
            binding = it
            viewPager = it.pagerSemester
            tabs = it.tabLayout
        }.apply {
            setLifecycleOwner(this@DisciplineFragment)
            viewModel = this@DisciplineFragment.viewModel
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SemesterAdapter(childFragmentManager)
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.semesters.observe(this, Observer {
            adapter.submitList(it)
        })
    }

    private inner class SemesterAdapter(fm: FragmentManager):  FragmentPagerAdapter(fm) {
        private val semesters: MutableList<Semester> = ArrayList()

        fun submitList(list: List<Semester>) {
            semesters.clear()
            semesters.addAll(list)
            notifyDataSetChanged()
        }

        override fun getCount() = semesters.size
        override fun getItem(position: Int) = DisciplineSemesterFragment.newInstance(semesters[position])
        override fun getPageTitle(position: Int) = semesters[position].codename.makeSemester()
    }
}
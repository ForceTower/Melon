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

package com.forcetower.uefs.feature.disciplines.disciplinedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ExtItemDisciplineHoursBinding
import com.forcetower.uefs.databinding.ExtItemMissedClassesBinding
import com.forcetower.uefs.databinding.FragmentDisciplineDetailsBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.disciplines.disciplinedetail.classes.ClassesFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.materials.MaterialsFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.overview.OverviewFragment
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.inflate
import com.forcetower.uefs.feature.shared.provideActivityViewModel
import com.forcetower.uefs.widget.DividerItemDecorator
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.CollectionReference
import javax.inject.Inject
import javax.inject.Named

class DisciplineDetailsFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    @field:Named(Discipline.COLLECTION)
    lateinit var firestore: CollectionReference

    private lateinit var viewModel: DisciplineViewModel
    private lateinit var binding: FragmentDisciplineDetailsBinding
    private lateinit var tabs: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var adapter: DetailsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        binding = FragmentDisciplineDetailsBinding.inflate(inflater, container, false).also {
            viewPager = it.viewPager
            tabs = it.tabs
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = DetailsAdapter(childFragmentManager)
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)
        viewPager.offscreenPageLimit = 4

        val menuAdapter = ItemsDisciplineAdapter()
        binding.recyclerDisciplineItems.apply {
            adapter = menuAdapter
            addItemDecoration(DividerItemDecorator(context.getDrawable(R.drawable.divider)!!, DividerItemDecoration.HORIZONTAL))
        }

        binding.up.setOnClickListener {
            activity?.finishAfterTransition()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.setClassId(requireNotNull(arguments).getLong(DisciplineDetailsActivity.CLASS_ID))
        viewModel.setClassGroupId(requireNotNull(arguments).getLong(DisciplineDetailsActivity.CLASS_GROUP_ID))
        viewModel.loadClassDetails.observe(this, Observer { Unit })
        binding.apply {
            viewModel = this@DisciplineDetailsFragment.viewModel
            setLifecycleOwner(this@DisciplineDetailsFragment)
        }

        createFragments()
    }

    private fun createFragments() {
        val group = requireNotNull(arguments).getLong(DisciplineDetailsActivity.CLASS_GROUP_ID)
        val overview = getString(R.string.discipline_details_overview) to OverviewFragment.newInstance(group)
        val classes = getString(R.string.discipline_details_classes) to ClassesFragment.newInstance(group)
        val materials = getString(R.string.discipline_details_materials) to MaterialsFragment.newInstance(group)
        val list = listOf<Pair<String, Fragment>>(overview, classes, materials)
        adapter.submitList(list)
    }

    private inner class DetailsAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val tabs = mutableListOf<Pair<String, Fragment>>()
        override fun getItem(position: Int) = tabs[position].second
        override fun getCount() = tabs.size
        override fun getPageTitle(position: Int) = tabs[position].first
        fun submitList(pairs: List<Pair<String, Fragment>>) {
            tabs.clear()
            tabs.addAll(pairs)
            notifyDataSetChanged()
        }
    }

    companion object {
        fun newInstance(classId: Long, classGroupId: Long): DisciplineDetailsFragment {
            return DisciplineDetailsFragment().apply {
                arguments = bundleOf(
                    DisciplineDetailsActivity.CLASS_GROUP_ID to classGroupId,
                    DisciplineDetailsActivity.CLASS_ID to classId
                )
            }
        }
    }

    private inner class ItemsDisciplineAdapter : RecyclerView.Adapter<ItemHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            return when (viewType) {
                0 -> ItemHolder.HoursHolder(parent.inflate(R.layout.ext_item_discipline_hours))
                1 -> ItemHolder.MissedHolder(parent.inflate(R.layout.ext_item_missed_classes))
                else -> throw IllegalStateException("Invalid State of views")
            }
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            if (::viewModel.isInitialized) {
                when (holder) {
                    is ItemHolder.HoursHolder -> holder.binding.apply {
                        viewModel = this@DisciplineDetailsFragment.viewModel
                        setLifecycleOwner(this@DisciplineDetailsFragment)
                        executePendingBindings()
                    }
                    is ItemHolder.MissedHolder -> holder.binding.apply {
                        viewModel = this@DisciplineDetailsFragment.viewModel
                        setLifecycleOwner(this@DisciplineDetailsFragment)
                        executePendingBindings()
                    }
                }
            }
        }

        override fun getItemCount() = 2
        override fun getItemViewType(position: Int) = position
    }

    private sealed class ItemHolder(item: View) : RecyclerView.ViewHolder(item) {
        class HoursHolder(val binding: ExtItemDisciplineHoursBinding) : ItemHolder(binding.root)
        class MissedHolder(val binding: ExtItemMissedClassesBinding) : ItemHolder(binding.root)
    }
}
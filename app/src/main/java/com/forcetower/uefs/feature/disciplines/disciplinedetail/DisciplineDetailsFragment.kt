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

package com.forcetower.uefs.feature.disciplines.disciplinedetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.forcetower.core.lifecycle.EventObserver
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.databinding.ExtItemDisciplineHoursBinding
import com.forcetower.uefs.databinding.ExtItemMissedClassesBinding
import com.forcetower.uefs.databinding.FragmentDisciplineDetailsBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.disciplines.disciplinedetail.absences.AbsencesFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.classes.ClassesFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.materials.MaterialsFragment
import com.forcetower.uefs.feature.disciplines.disciplinedetail.overview.OverviewFragment
import com.forcetower.uefs.feature.evaluation.EvaluationActivity
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.extensions.openURL
import com.forcetower.uefs.feature.shared.inflate
import com.forcetower.uefs.widget.DividerItemDecorator
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.CollectionReference
import javax.inject.Inject
import javax.inject.Named

class DisciplineDetailsFragment : UFragment() {
    @Inject @Named(Discipline.COLLECTION)
    lateinit var firestore: CollectionReference

    private val viewModel: DisciplineViewModel by activityViewModels()
    private lateinit var binding: FragmentDisciplineDetailsBinding
    private lateinit var tabs: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var adapter: DetailsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDisciplineDetailsBinding.inflate(inflater, container, false).also {
            viewPager = it.viewPager
            tabs = it.tabs
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = DetailsAdapter(childFragmentManager)
        createFragments()
        viewPager.adapter = adapter
        tabs.setupWithViewPager(viewPager)
        viewPager.offscreenPageLimit = 4

        val menuAdapter = ItemsDisciplineAdapter()
        binding.recyclerDisciplineItems.apply {
            adapter = menuAdapter
            addItemDecoration(DividerItemDecorator(ContextCompat.getDrawable(context, R.drawable.divider)!!, DividerItemDecoration.HORIZONTAL))
        }

        binding.up.setOnClickListener {
            activity?.finishAfterTransition()
        }

        viewModel.materialClick.observe(viewLifecycleOwner, EventObserver { requireContext().openURL(it.link) })

        viewModel.setClassId(requireNotNull(arguments).getLong(DisciplineDetailsActivity.CLASS_ID))
        viewModel.setClassGroupId(requireNotNull(arguments).getLong(DisciplineDetailsActivity.CLASS_GROUP_ID))
        viewModel.navigateToTeacherAction.observe(
            viewLifecycleOwner,
            EventObserver {
                startActivity(EvaluationActivity.startIntentForTeacher(requireContext(), it))
            }
        )
        binding.apply {
            viewModel = this@DisciplineDetailsFragment.viewModel
            lifecycleOwner = this@DisciplineDetailsFragment
        }
    }

    private fun createFragments() {
        val group = requireNotNull(arguments).getLong(DisciplineDetailsActivity.CLASS_GROUP_ID)
        val overview = getString(R.string.discipline_details_overview) to OverviewFragment.newInstance(group)
        val classes = getString(R.string.discipline_details_classes) to ClassesFragment.newInstance(group)
        val materials = getString(R.string.discipline_details_materials) to MaterialsFragment.newInstance(group)
        val absences = getString(R.string.discipline_details_absences) to AbsencesFragment.newInstance(group)
        val list = listOf<Pair<String, Fragment>>(overview, classes, materials, absences)
        adapter.submitList(list)
    }

    private class DetailsAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
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
            when (holder) {
                is ItemHolder.HoursHolder -> holder.binding.apply {
                    viewModel = this@DisciplineDetailsFragment.viewModel
                    lifecycleOwner = this@DisciplineDetailsFragment
                    executePendingBindings()
                }
                is ItemHolder.MissedHolder -> holder.binding.apply {
                    viewModel = this@DisciplineDetailsFragment.viewModel
                    lifecycleOwner = this@DisciplineDetailsFragment
                    executePendingBindings()
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

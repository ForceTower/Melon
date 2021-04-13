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

package dev.forcetower.disciplines.feature.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.forcetower.uefs.core.injection.dependencies.DisciplineModuleDependencies
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.util.fromJson
import com.forcetower.uefs.feature.shared.RoundedDialog
import com.google.android.play.core.splitcompat.SplitCompat
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.disciplines.core.injection.DaggerDisciplineComponent
import dev.forcetower.disciplines.databinding.DialogSelectDisciplineGroupBinding
import dev.forcetower.disciplines.feature.DisciplinesViewModel
import java.lang.IllegalStateException
import javax.inject.Inject

class SelectGroupDialog : RoundedDialog() {
    @Inject lateinit var factory: ViewModelProvider.Factory
    private val viewModel: DisciplinesViewModel by activityViewModels { factory }
    private lateinit var binding: DialogSelectDisciplineGroupBinding
    private lateinit var value: ClassFullWithGroup

    override fun onAttach(context: Context) {
        super.onAttach(context)
        SplitCompat.install(context)
        DaggerDisciplineComponent.builder()
            .context(context)
            .dependencies(
                EntryPointAccessors.fromActivity(
                    requireActivity(),
                    DisciplineModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)
    }

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        value = requireArguments().getString("groups")?.fromJson() ?: throw IllegalStateException("Argument groups was not defined")
        return DialogSelectDisciplineGroupBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            clazzGroups = value
            executePendingBindings()
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = value.groups
        val groupsAdapter = DisciplineGroupsAdapter(this, viewModel)
        binding.recyclerGroups.apply {
            adapter = groupsAdapter
        }
        groupsAdapter.submitList(list)
    }
}

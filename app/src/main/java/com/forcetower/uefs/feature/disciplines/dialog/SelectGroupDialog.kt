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

package com.forcetower.uefs.feature.disciplines.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.forcetower.uefs.core.storage.database.aggregation.ClassFullWithGroup
import com.forcetower.uefs.core.util.fromJson
import com.forcetower.uefs.databinding.DialogSelectDisciplineGroupOldBinding
import com.forcetower.uefs.feature.disciplines.DisciplineViewModel
import com.forcetower.uefs.feature.shared.RoundedDialog
import dagger.hilt.android.AndroidEntryPoint
import java.lang.IllegalStateException

@AndroidEntryPoint
class SelectGroupDialog : RoundedDialog() {
    private val viewModel: DisciplineViewModel by activityViewModels()
    private lateinit var binding: DialogSelectDisciplineGroupOldBinding
    private lateinit var value: ClassFullWithGroup

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        value = requireNotNull(arguments).getString("groups")?.fromJson() ?: throw IllegalStateException("Argument groups was not defined")
        return DialogSelectDisciplineGroupOldBinding.inflate(inflater, container, false).also {
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

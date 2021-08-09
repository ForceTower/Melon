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

package com.forcetower.uefs.feature.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.uefs.core.model.unes.Contributor
import com.forcetower.uefs.core.storage.repository.ContributorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContributorViewModel @Inject constructor(
    private val repository: ContributorRepository
) : ViewModel(), ContributorActions {
    val contributors by lazy { repository.loadContributors() }

    private val _contributorClickAction = MutableLiveData<Event<String>>()
    val contributorClickAction: LiveData<Event<String>>
        get() = _contributorClickAction

    override fun onContributorClick(contributor: Contributor?) {
        val link = contributor?.link ?: return
        _contributorClickAction.value = Event(link)
    }
}

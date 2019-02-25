/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.feature.barrildeboa

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.model.api.UDiscipline
import com.forcetower.uefs.core.model.api.UDisciplineWithData
import com.forcetower.uefs.core.storage.repository.api.HourglassRepository
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.shared.extensions.setValueIfNew
import javax.inject.Inject

class HourglassViewModel @Inject constructor(
    private val repository: HourglassRepository
) : ViewModel(), ElementInteractor {
    private val _onSelectDiscipline = MutableLiveData<Event<UDiscipline>>()
    val onSelectDiscipline: LiveData<Event<UDiscipline>>
        get() = _onSelectDiscipline

    private val _queryDiscipline = MediatorLiveData<List<UDiscipline>>()
    val queryDiscipline: LiveData<List<UDiscipline>>
        get() = _queryDiscipline

    private var currentSource: LiveData<List<UDiscipline>>? = null

    private val disciplineCode = MutableLiveData<String?>()

    private val _discipline = MediatorLiveData<UDisciplineWithData?>()
    val discipline: LiveData<UDisciplineWithData?>
        get() = _discipline

    init {
        _discipline.addSource(disciplineCode) {
            if (it != null) {
                refreshCurrentDiscipline(it)
            }
        }
    }

    private fun refreshCurrentDiscipline(code: String) {
        val source = repository.getDisciplineDetails(code)
        _discipline.addSource(source) {
            _discipline.value = it?.data?.data
        }
    }

    fun sendData() = repository.sendData()
    fun overview() = repository.overview()

    override fun onSelectDiscipline(discipline: UDiscipline) {
        _onSelectDiscipline.value = Event(discipline)
    }

    fun query(query: String?) {
        val source = currentSource
        if (source != null) {
            _queryDiscipline.removeSource(source)
        }

        val newSource = repository.query(query)
        currentSource = newSource
        _queryDiscipline.addSource(newSource) {
            _queryDiscipline.value = it
        }
    }

    fun setDisciplineCode(code: String?) {
        disciplineCode.setValueIfNew(code)
    }
}
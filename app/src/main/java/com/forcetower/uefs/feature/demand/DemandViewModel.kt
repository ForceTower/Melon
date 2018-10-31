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

package com.forcetower.uefs.feature.demand

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.uefs.core.storage.repository.DemandRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import timber.log.Timber
import javax.inject.Inject

class DemandViewModel @Inject constructor(
    private val repository: DemandRepository
): ViewModel(), OfferActions {
    private var loaded = false

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading

    private val _offers = MediatorLiveData<Resource<List<SDemandOffer>>>()
    val offers: LiveData<Resource<List<SDemandOffer>>>
        get() {
            if (!loaded) { initLoad() }
            return _offers
        }

    private val _selectedCount = MutableLiveData<Int>()
    val selectedCount: LiveData<Int>
        get() = _selectedCount

    private val _selectedHours = MutableLiveData<Int>()
    val selectedHours: LiveData<Int>
        get() = _selectedHours

    private fun initLoad() {
        loaded = true
        _loading.value = true
        val source = repository.loadDemand()
        _offers.addSource(source) {
            _offers.value = it
            val raw = it.data
            if (raw != null) {
                val filtered = raw.filter { s -> s.selected }
                _selectedHours.value = filtered.sumBy { h -> h.hours }
                _selectedCount.value = filtered.size
            }

            if (it.status == Status.SUCCESS || it.status == Status.ERROR) {
                _loading.value = false
            }
        }
    }

    init {

    }

    override fun onOfferClick(offer: SDemandOffer) {
        Timber.d("Offer clicked: ${offer.code}")
    }

    override fun onOfferLongClick(offer: SDemandOffer) {
        Timber.d("Offer long clicked: ${offer.code}")
    }

    override fun onConfirmOffers() {
        Timber.d("Confirm offers!")
    }

    override fun onClearOffers() {
        Timber.d("Clear all offers")
    }
}
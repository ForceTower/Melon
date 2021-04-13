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

package com.forcetower.uefs.feature.demand

import android.content.Context
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.core.lifecycle.Event
import com.forcetower.sagres.database.model.SagresDemandOffer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.repository.DemandRepository
import com.forcetower.uefs.core.storage.resource.Resource
import com.forcetower.uefs.core.storage.resource.Status
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DemandViewModel @Inject constructor(
    private val repository: DemandRepository,
    private val context: Context,
    private val analytics: FirebaseAnalytics
) : ViewModel(), OfferActions {
    private var loaded = false

    private val _snackbar = MutableLiveData<Event<String>>()
    val snackbarMessage: LiveData<Event<String>>
        get() = _snackbar

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean>
        get() = _loading

    private val _offers = MediatorLiveData<Resource<List<SagresDemandOffer>>>()
    val offers: LiveData<Resource<List<SagresDemandOffer>>>
        get() {
            if (!loaded) { initLoad() }
            return _offers
        }

    private val _selectedCount = MutableLiveData<Int>()
    val selectedCount: LiveData<Int>
        get() = _selectedCount

    val selected = repository.getSelected()

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
                if (it.status == Status.SUCCESS) {
                    analytics.logEvent("demand_loaded_disciplines_success", null)
                } else {
                    analytics.logEvent(
                        "demand_loaded_disciplines_failed",
                        bundleOf(
                            "code" to it.code,
                            "message" to (it.message ?: "nothing at all")
                        )
                    )
                }
                _loading.value = false
            }
        }
    }

    override fun onOfferClick(offer: SagresDemandOffer) {
        Timber.d("Offer clicked: ${offer.code}")
        if (!offer.selectable || offer.completed || offer.unavailable) {
            Timber.d("Select something valid")
            showSnack(context.getString(R.string.demand_select_valid_discipline))
            return
        }

        val select = !offer.selected
        repository.updateOfferSelection(offer, select)
    }

    override fun onOfferLongClick(offer: SagresDemandOffer): Boolean {
        Timber.d("Offer long clicked: ${offer.code}")
        return true
    }

    override fun onConfirmOffers() {
        Timber.d("Confirm offers!")
        repository.confirmOptions()
        analytics.logEvent("demand_confirmed_offers", bundleOf("amount" to _selectedCount.value))
        showSnack(context.getString(R.string.demand_will_be_created_notification))
    }

    override fun onClearOffers() {
        Timber.d("Clear all offers")
    }

    fun showSnack(message: String) {
        _snackbar.value = Event(message)
    }
}

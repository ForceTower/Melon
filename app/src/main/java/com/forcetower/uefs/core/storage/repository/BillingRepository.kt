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

package com.forcetower.uefs.core.storage.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.android.billingclient.api.Purchase
import com.forcetower.uefs.core.effects.purchases.PurchaseEffect
import com.forcetower.uefs.core.effects.purchases.SubscriptionEffect
import com.forcetower.uefs.core.storage.database.UDatabase
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Named

class BillingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val database: UDatabase,
    @Named("scoreIncreaseEffect") private val scoreIncreaseEffect: PurchaseEffect,
    @Named("monkeyGoldEffect") private val monkeyGoldEffect: SubscriptionEffect
) {

    fun getUsername(): LiveData<String?> {
        val source = database.accessDao().getAccess()
        return Transformations.map(source) { it?.username }
    }

    fun getManagedSkus(): LiveData<List<String>> {
        val result = MutableLiveData<List<String>>()
        firestore.collection("products_sku").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val data = snapshot.documents.map { it.data?.get("sku") as String }
                result.postValue(data)
            }
        }
        return result
    }

    fun handlePurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            // TODO Make the effects consume the token if needed (should billing client be moved to dagger? Hum....)
            when (purchase.sku) {
                "score_increase_common" -> scoreIncreaseEffect.runEffect()
                "unes_gold_monkey" -> monkeyGoldEffect.runEffect()
            }
        }
    }

    fun isGoldMonkey(): Boolean {
        return monkeyGoldEffect.isEffectActive()
    }

    fun cancelSubscriptions() {
        monkeyGoldEffect.removeEffect()
    }
}

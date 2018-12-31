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

package com.forcetower.uefs.core.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.SkuDetails
import com.forcetower.uefs.core.billing.BillingClientLifecycle
import com.forcetower.uefs.core.storage.repository.BillingRepository
import javax.inject.Inject

class BillingViewModel @Inject constructor(
    val client: BillingClientLifecycle,
    val repository: BillingRepository
) : ViewModel() {
    private val _selectSku = MutableLiveData<Event<SkuDetails>>()
    val selectSku: LiveData<Event<SkuDetails>>
        get() = _selectSku

    val state = client.state
    val purchases = client.purchases
    val purchaseUpdateEvent = client.purchaseUpdateEvent
    fun querySkuDetails(list: List<String>) = client.querySkuDetails(list)
    fun consumeItem(token: String) = client.consumeToken(token)

    fun getSkus() = repository.getManagedSkus()

    fun selectSku(skuDetails: SkuDetails?) {
        skuDetails ?: return
        _selectSku.value = Event(skuDetails)
    }
}
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

package com.forcetower.sagres.operation.demand

import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Status

class DemandOffersCallback(status: Status): BaseCallback<DemandOffersCallback>(status) {
    private var offers: List<SDemandOffer>? = null

    fun getOffers() = offers

    fun offers(offers: List<SDemandOffer>?): DemandOffersCallback {
        this.offers = offers
        return this
    }

    companion object {
        fun copyFrom(callback: BaseCallback<*>): DemandOffersCallback {
            return DemandOffersCallback(callback.status).message(callback.message).code(callback.code).throwable(
                callback.throwable).document(callback.document)
        }
    }
}
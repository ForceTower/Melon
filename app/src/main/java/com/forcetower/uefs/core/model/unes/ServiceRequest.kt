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

package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.forcetower.sagres.database.model.SRequestedService

/**
 * Notify status:
 * 0 -> Nothing new
 * 1 -> Created
 * 2 -> Updated
 */
@Entity(indices = [
    Index(value = ["service", "date"], name = "service_uniqueness", unique = true)
])
data class ServiceRequest(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val service: String,
    val date: String,
    val amount: Int,
    var situation: String,
    val value: String,
    var observation: String,
    var notify: Int
) {
    companion object {
        fun fromSagres(request: SRequestedService): ServiceRequest {
            return ServiceRequest(
                0,
                request.service,
                request.date,
                request.amount,
                request.situation,
                request.value,
                request.observation,
                1
            )
        }
    }
}
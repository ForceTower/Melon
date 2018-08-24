/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.sagres.database.model

data class SGrade(
    val semesterId: Long,
    val discipline: String,
    var partialMean: String = "",
    var finalScore: String = ""
) {
    val values: MutableList<SGradeInfo> = ArrayList()

    fun addInfo(info: SGradeInfo) {
        values.add(info)
    }

    override fun toString(): String = "$discipline has $values"
}
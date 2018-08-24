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

data class SGradeInfo(
    val name: String,
    val grade: String,
    val date: String,
    val weight: Double
) {
    override fun toString(): String {
        return "$name: $grade"
    }

    fun hasGrade(): Boolean {
        return (!grade.trim { it <= ' ' }.isEmpty()
                && !grade.trim { it <= ' ' }.equals("Não Divulgada", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("-", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("--", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("*", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("**", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("-1", ignoreCase = true))
    }

    fun hasDate(): Boolean {
        return (!grade.trim { it <= ' ' }.isEmpty()
                && !grade.trim { it <= ' ' }.equals("Não Divulgada", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("Não informada", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("-", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("--", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("*", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("**", ignoreCase = true)
                && !grade.trim { it <= ' ' }.equals("-1", ignoreCase = true))
    }
}
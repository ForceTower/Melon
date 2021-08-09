/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2021. João Paulo Sena <joaopaulo761@gmail.com>
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

package dev.forcetower.unes.usecase

sealed class UCaseResult<out R> {
    data class Success<out T>(val data: T) : UCaseResult<T>()
    data class Error(val exception: Exception) : UCaseResult<Nothing>()
    object Loading : UCaseResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            Loading -> "Loading"
        }
    }
}

val UCaseResult<*>.succeeded
    get() = this is UCaseResult.Success && data != null

fun <T> UCaseResult<T>.successOr(fallback: T): T {
    return (this as? UCaseResult.Success<T>)?.data ?: fallback
}

val <T> UCaseResult<T>.data: T?
    get() = (this as? UCaseResult.Success)?.data

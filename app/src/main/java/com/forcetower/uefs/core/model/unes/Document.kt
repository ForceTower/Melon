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

package com.forcetower.uefs.core.model.unes

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [
        Index(value = ["type"], unique = true)
    ]
)
data class SagresDocument(
    @PrimaryKey(autoGenerate = true)
    val uid: Long = 0,
    val type: String,
    val name: String,
    val downloaded: Boolean = false,
    val downloading: Boolean = false,
    val date: Long = System.currentTimeMillis()
) {
    companion object {
        fun enrollment() = SagresDocument(type = Document.ENROLLMENT.value, name = "Guia de Matrícula")
        fun flowchart() = SagresDocument(type = Document.FLOWCHART.value, name = "Fluxograma")
        fun history() = SagresDocument(type = Document.HISTORY.value, name = "Histórico Escolar")
    }
}

// TODO This is just bad... Android doesn't do well with enums. Change this!
enum class Document(val value: String) {
    ENROLLMENT("enrollment.pdf"),
    FLOWCHART("flowchart.pdf"),
    HISTORY("history.pdf")
}

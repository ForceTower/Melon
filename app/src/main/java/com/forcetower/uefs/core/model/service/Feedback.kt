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

package com.forcetower.uefs.core.model.service

import com.forcetower.uefs.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class Feedback(
    @Exclude var id: String = "",
    var text: String = "",
    var awswered: Boolean = false,
    var username: String = "random",
    var hash: String = "hashed",
    var email: String? = "email",
    var course: Long? = null,
    var firebaseId: String? = null,
    var manufacturer: String? = null,
    var deviceModel: String? = null,
    var android: Int? = 1,
    var versionCode: Int = BuildConfig.VERSION_CODE,
    var currentToken: String? = null,
    @ServerTimestamp var createdAt: Timestamp? = null
) {

    companion object {
        const val COLLECTION = "feedback_first"
    }
}

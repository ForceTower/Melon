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

package com.forcetower.uefs.core.constants

object Constants {
    const val SELECTED_INSTITUTION_KEY = "selected_institution_worker"
    const val UNES_SERVICE_BASE_URL = "unes.forcetower.dev"
    private const val UNES_SERVICE_BASE_UPDATE = "unes.herokuapp.com"
    const val UNES_SERVICE_URL = "https://$UNES_SERVICE_BASE_URL/api/"
    const val UNES_SERVICE_UPDATE = "http://$UNES_SERVICE_BASE_UPDATE/api/"

    const val DEVELOPER_EMAIL = "joaopaulo761@gmail.com"
    const val REMOTE_CONFIG_REFRESH = 900L

    val HARD_DISCIPLINES = mapOf("TEC501" to "__ANY__")
    val EXECUTOR_WHITELIST = listOf("manual", "universal")

    const val ADMOB_TEST_ID = "38D27336B4D54E6E431E86E4ABEE0B20"

    const val SERVICE_CLIENT_ID = "1"
    const val SERVICE_CLIENT_SECRET = "bCP23X90J5anU0H3uxzWg0RwE6BxEo0HDkqr0PZg"
    const val SERVICE_CLIENT_INSTITUTION = "uefs"

    object DynamicFeatures {
        const val AERI = "aeri"
        const val MAPS = "map"
    }
}

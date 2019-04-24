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

package com.forcetower.uefs.core.constants

object Constants {
    const val UNES_SERVICE_BASE_URL = "unes.herokuapp.com"
    private const val UNES_SERVICE_BASE_TEST = "unes-js.herokuapp.com"
    const val UNES_SERVICE_URL = "https://$UNES_SERVICE_BASE_URL/api/"
    const val UNES_SERVICE_TESTING = "https://$UNES_SERVICE_BASE_TEST/"

    const val DEVELOPER_EMAIL = "joaopaulo761@gmail.com"
    const val REMOTE_CONFIG_REFRESH = 900L

    val HARD_DISCIPLINES = mapOf("TEC501" to "__ANY__")
    val EXECUTOR_WHITELIST = listOf("manual", "universal")

    const val ADMOB_TEST_ID = "38D27336B4D54E6E431E86E4ABEE0B20"

    const val SERVICE_CLIENT_ID = "1"
    const val SERVICE_CLIENT_SECRET = "9qlodlQSCgIaSeQSf2Npl6GT8oeftfSG9bqMoDeZ"
}
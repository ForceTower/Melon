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

package com.forcetower.uefs.feature.siecomp

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import com.forcetower.uefs.core.util.siecomp.TimeUtils
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.siecomp.onboarding.OnboardingActivity
import com.forcetower.uefs.feature.siecomp.schedule.EventScheduleActivity

class SIECOMPActivity : UActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            if (TimeUtils.eventHasStarted() && preferences.getBoolean("siecomp_xxii_onboarding_completed_2", false)) {
                startActivity(Intent(this, EventScheduleActivity::class.java))
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }
        }
        finish()
    }
}

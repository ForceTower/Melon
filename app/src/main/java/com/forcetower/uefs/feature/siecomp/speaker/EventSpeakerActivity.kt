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

package com.forcetower.uefs.feature.siecomp.speaker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.forcetower.uefs.R
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.inTransaction
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EventSpeakerActivity : UActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_speaker)

        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                val speakerId = intent.getLongExtra(SPEAKER_ID, 1)
                add(R.id.speaker_container, SpeakerFragment.newInstance(speakerId))
            }
        }
    }

    companion object {
        const val SPEAKER_ID = "speaker_id"

        fun startIntent(context: Context, speakerId: Long): Intent {
            return Intent(context, EventSpeakerActivity::class.java).apply {
                putExtra(SPEAKER_ID, speakerId)
            }
        }
    }
}

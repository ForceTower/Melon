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

package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.core.model.service.ClassStatsData
import com.forcetower.uefs.core.model.service.Feedback
import com.forcetower.uefs.core.model.service.SyncFrequency
import com.forcetower.uefs.core.model.service.UMessage
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.core.model.unes.Profile
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {
    @Provides
    @Reusable
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Reusable
    @Named(Profile.COLLECTION)
    fun provideUserCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(Profile.COLLECTION)

    @Provides
    @Reusable
    @Named(Event.COLLECTION)
    fun provideEventCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(Event.COLLECTION)

    @Provides
    @Reusable
    @Named(Discipline.COLLECTION)
    fun provideDisciplineCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(Discipline.COLLECTION)

    @Provides
    @Reusable
    @Named(UMessage.COLLECTION)
    fun provideUnesMessagesCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(UMessage.COLLECTION)

    @Provides
    @Reusable
    @Named(ClassStatsData.STATS_CONTRIBUTION)
    fun provideStatsContributionCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(ClassStatsData.STATS_CONTRIBUTION)

    @Provides
    @Reusable
    @Named(Feedback.COLLECTION)
    fun provideFeedbackCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(Feedback.COLLECTION)

    @Provides
    @Reusable
    @Named(SyncFrequency.COLLECTION)
    fun provideSyncFrequencyCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(SyncFrequency.COLLECTION)
}

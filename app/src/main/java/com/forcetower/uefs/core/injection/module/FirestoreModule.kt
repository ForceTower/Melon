/*
 * Copyright (c) 2018.
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

package com.forcetower.uefs.core.injection.module

import com.forcetower.uefs.core.model.service.Event
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Profile
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.Module
import dagger.Provides
import dagger.Reusable
import javax.inject.Named

@Module
object FirestoreModule {
    @JvmStatic
    @Provides
    @Reusable
    fun provideFirestore(): FirebaseFirestore {
        val settings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()
        return FirebaseFirestore.getInstance().apply {
            firestoreSettings = settings
        }

    }

    @JvmStatic
    @Provides
    @Reusable
    @Named(Profile.COLLECTION)
    fun provideUserCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(Profile.COLLECTION)

    @JvmStatic
    @Provides
    @Reusable
    @Named(Event.COLLECTION)
    fun provideEventCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(Event.COLLECTION)


    @JvmStatic
    @Provides
    @Reusable
    @Named(Discipline.COLLECTION)
    fun provideDisciplineCollection(firestore: FirebaseFirestore): CollectionReference = firestore.collection(Discipline.COLLECTION)

}
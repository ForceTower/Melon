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

package com.forcetower.uefs.core.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.forcetower.sagres.database.model.SDemandOffer
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.AccessToken
import com.forcetower.uefs.core.model.unes.CalendarItem
import com.forcetower.uefs.core.model.unes.Class
import com.forcetower.uefs.core.model.unes.ClassAbsence
import com.forcetower.uefs.core.model.unes.ClassGroup
import com.forcetower.uefs.core.model.unes.ClassItem
import com.forcetower.uefs.core.model.unes.ClassLocation
import com.forcetower.uefs.core.model.unes.ClassMaterial
import com.forcetower.uefs.core.model.unes.Contributor
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Discipline
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.core.model.unes.SagresFlags
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.model.unes.SyncRegistry
import com.forcetower.uefs.core.model.unes.Teacher
import com.forcetower.uefs.core.storage.database.dao.AccessDao
import com.forcetower.uefs.core.storage.database.dao.AccessTokenDao
import com.forcetower.uefs.core.storage.database.dao.CalendarDao
import com.forcetower.uefs.core.storage.database.dao.ClassAbsenceDao
import com.forcetower.uefs.core.storage.database.dao.ClassDao
import com.forcetower.uefs.core.storage.database.dao.ClassGroupDao
import com.forcetower.uefs.core.storage.database.dao.ClassItemDao
import com.forcetower.uefs.core.storage.database.dao.ClassLocationDao
import com.forcetower.uefs.core.storage.database.dao.ClassMaterialDao
import com.forcetower.uefs.core.storage.database.dao.ContributorDao
import com.forcetower.uefs.core.storage.database.dao.CourseDao
import com.forcetower.uefs.core.storage.database.dao.DemandOfferDao
import com.forcetower.uefs.core.storage.database.dao.DisciplineDao
import com.forcetower.uefs.core.storage.database.dao.DocumentDao
import com.forcetower.uefs.core.storage.database.dao.FlagsDao
import com.forcetower.uefs.core.storage.database.dao.GradeDao
import com.forcetower.uefs.core.storage.database.dao.MessageDao
import com.forcetower.uefs.core.storage.database.dao.ProfileDao
import com.forcetower.uefs.core.storage.database.dao.SemesterDao
import com.forcetower.uefs.core.storage.database.dao.ServiceRequestDao
import com.forcetower.uefs.core.storage.database.dao.SyncRegistryDao

@Database(entities = [
    AccessToken::class,
    Access::class,
    Profile::class,
    Semester::class,
    Message::class,
    CalendarItem::class,
    Discipline::class,
    Class::class,
    ClassGroup::class,
    ClassAbsence::class,
    ClassLocation::class,
    ClassItem::class,
    ClassMaterial::class,
    Grade::class,
    Course::class,
    SagresDocument::class,
    SyncRegistry::class,
    Teacher::class,
    SDemandOffer::class,
    SagresFlags::class,
    Contributor::class,
    ServiceRequest::class
], version = 19, exportSchema = true)
abstract class UDatabase : RoomDatabase() {
    abstract fun accessDao(): AccessDao
    abstract fun accessTokenDao(): AccessTokenDao
    abstract fun profileDao(): ProfileDao
    abstract fun messageDao(): MessageDao
    abstract fun semesterDao(): SemesterDao
    abstract fun calendarDao(): CalendarDao
    abstract fun disciplineDao(): DisciplineDao
    abstract fun classDao(): ClassDao
    abstract fun classGroupDao(): ClassGroupDao
    abstract fun classAbsenceDao(): ClassAbsenceDao
    abstract fun classLocationDao(): ClassLocationDao
    abstract fun gradesDao(): GradeDao
    abstract fun courseDao(): CourseDao
    abstract fun documentDao(): DocumentDao
    abstract fun syncRegistryDao(): SyncRegistryDao
    abstract fun classMaterialDao(): ClassMaterialDao
    abstract fun classItemDao(): ClassItemDao
    abstract fun demandOfferDao(): DemandOfferDao
    abstract fun flagsDao(): FlagsDao
    abstract fun contributorDao(): ContributorDao
    abstract fun serviceRequestDao(): ServiceRequestDao
}
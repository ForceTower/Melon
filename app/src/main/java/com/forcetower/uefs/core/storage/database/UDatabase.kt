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

package com.forcetower.uefs.core.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.forcetower.uefs.core.model.unes.Access
import com.forcetower.uefs.core.model.unes.AccessToken
import com.forcetower.uefs.core.model.unes.Account
import com.forcetower.uefs.core.model.unes.AffinityQuestion
import com.forcetower.uefs.core.model.unes.AffinityQuestionAlternative
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
import com.forcetower.uefs.core.model.unes.EvaluationEntity
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.core.model.unes.Flowchart
import com.forcetower.uefs.core.model.unes.FlowchartDiscipline
import com.forcetower.uefs.core.model.unes.FlowchartRequirement
import com.forcetower.uefs.core.model.unes.FlowchartSemester
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.core.model.unes.Message
import com.forcetower.uefs.core.model.unes.Profile
import com.forcetower.uefs.core.model.unes.ProfileStatement
import com.forcetower.uefs.core.model.unes.SDemandOffer
import com.forcetower.uefs.core.model.unes.SDiscipline
import com.forcetower.uefs.core.model.unes.SStudent
import com.forcetower.uefs.core.model.unes.STeacher
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.core.model.unes.SagresFlags
import com.forcetower.uefs.core.model.unes.Semester
import com.forcetower.uefs.core.model.unes.ServiceRequest
import com.forcetower.uefs.core.model.unes.SyncRegistry
import com.forcetower.uefs.core.model.unes.Teacher
import com.forcetower.uefs.core.model.unes.UserSession
import com.forcetower.uefs.core.storage.database.dao.AccessDao
import com.forcetower.uefs.core.storage.database.dao.AccessTokenDao
import com.forcetower.uefs.core.storage.database.dao.AccountDao
import com.forcetower.uefs.core.storage.database.dao.AffinityQuestionDao
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
import com.forcetower.uefs.core.storage.database.dao.DisciplineServiceDao
import com.forcetower.uefs.core.storage.database.dao.DocumentDao
import com.forcetower.uefs.core.storage.database.dao.EvaluationEntitiesDao
import com.forcetower.uefs.core.storage.database.dao.EventDao
import com.forcetower.uefs.core.storage.database.dao.FlagsDao
import com.forcetower.uefs.core.storage.database.dao.FlowchartDao
import com.forcetower.uefs.core.storage.database.dao.FlowchartDisciplineDao
import com.forcetower.uefs.core.storage.database.dao.FlowchartRequirementDao
import com.forcetower.uefs.core.storage.database.dao.FlowchartSemesterDao
import com.forcetower.uefs.core.storage.database.dao.GradeDao
import com.forcetower.uefs.core.storage.database.dao.MessageDao
import com.forcetower.uefs.core.storage.database.dao.ProfileDao
import com.forcetower.uefs.core.storage.database.dao.ProfileStatementDao
import com.forcetower.uefs.core.storage.database.dao.SemesterDao
import com.forcetower.uefs.core.storage.database.dao.ServiceRequestDao
import com.forcetower.uefs.core.storage.database.dao.StudentServiceDao
import com.forcetower.uefs.core.storage.database.dao.SyncRegistryDao
import com.forcetower.uefs.core.storage.database.dao.TeacherDao
import com.forcetower.uefs.core.storage.database.dao.TeacherServiceDao
import com.forcetower.uefs.core.storage.database.dao.UserSessionDao
import com.forcetower.uefs.core.util.Converters

@Database(
    entities = [
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
        ServiceRequest::class,
        Account::class,
        STeacher::class,
        SDiscipline::class,
        SStudent::class,
        EvaluationEntity::class,
        Flowchart::class,
        FlowchartSemester::class,
        FlowchartDiscipline::class,
        FlowchartRequirement::class,
        ProfileStatement::class,
        UserSession::class,
        AffinityQuestion::class,
        AffinityQuestionAlternative::class,
        Event::class
    ],
    version = 52,
    exportSchema = true
)
@TypeConverters(value = [Converters::class])
abstract class UDatabase : RoomDatabase() {
    abstract fun accessDao(): AccessDao
    abstract fun accessTokenDao(): AccessTokenDao
    abstract fun profileDao(): ProfileDao
    abstract fun messageDao(): MessageDao
    abstract fun semesterDao(): SemesterDao
    abstract fun calendarDao(): CalendarDao
    abstract fun disciplineDao(): DisciplineDao
    abstract fun classDao(): ClassDao
    abstract fun teacherDao(): TeacherDao
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
    abstract fun accountDao(): AccountDao
    abstract fun disciplineServiceDao(): DisciplineServiceDao
    abstract fun teacherServiceDao(): TeacherServiceDao
    abstract fun studentServiceDao(): StudentServiceDao
    abstract fun evaluationEntitiesDao(): EvaluationEntitiesDao
    abstract fun flowchartDao(): FlowchartDao
    abstract fun flowchartSemesterDao(): FlowchartSemesterDao
    abstract fun flowchartDisciplineDao(): FlowchartDisciplineDao
    abstract fun flowchartRequirementDao(): FlowchartRequirementDao
    abstract fun statementDao(): ProfileStatementDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun affinityQuestion(): AffinityQuestionDao
    abstract fun eventDao(): EventDao
}

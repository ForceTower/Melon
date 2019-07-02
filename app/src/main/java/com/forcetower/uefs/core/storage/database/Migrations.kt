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

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object M1TO2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val sagresFlagsTable = "SagresFlags"
        val demandOfferTable = "SDemandOffer"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$sagresFlagsTable` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `demand_open` INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `$demandOfferTable` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `selected` INTEGER NOT NULL, `category` TEXT NOT NULL, `hours` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `available` INTEGER NOT NULL, `current` INTEGER NOT NULL, `selectable` INTEGER NOT NULL, `unavailable` INTEGER NOT NULL)")
        database.execSQL("INSERT INTO `$sagresFlagsTable` (`uid`, `demand_open`) VALUES (1, 0)")
    }
}

object M2TO3 : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Message ADD COLUMN discipline TEXT DEFAULT NULL")
    }
}

object M3TO4 : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM ClassMaterial")
        database.execSQL("UPDATE ClassGroup SET draft = 1")
        database.execSQL("ALTER TABLE Discipline ADD COLUMN resume TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE Discipline ADD COLUMN short_text TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE Message ADD COLUMN code_discipline TEXT DEFAULT NULL")
    }
}

object M5TO6 : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val tableName = "Contributor"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$tableName` (`id` INTEGER NOT NULL, `login` TEXT NOT NULL, `total` INTEGER NOT NULL, `name` TEXT NOT NULL, `image` TEXT, `link` TEXT, `url` TEXT, `bio` TEXT, PRIMARY KEY(`id`))")
        database.execSQL("CREATE UNIQUE INDEX `index_Contributor_login` ON `$tableName` (`login`)")
    }
}

object M6TO7 : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val tableName = "Profile"
        database.execSQL("ALTER TABLE $tableName ADD COLUMN calc_score REAL NOT NULL DEFAULT -1")
    }
}

object M7TO8 : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Class ADD COLUMN schedule_only INTEGER NOT NULL DEFAULT 0")
    }
}

object M8TO9 : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS ServiceRequest (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `service` TEXT NOT NULL, `date` TEXT NOT NULL, `amount` INTEGER NOT NULL, `situation` TEXT NOT NULL, `value` TEXT NOT NULL, `observation` TEXT NOT NULL, `notify` INTEGER NOT NULL)")
        database.execSQL("CREATE UNIQUE INDEX `service_uniqueness` ON ServiceRequest (`service`, `date`)")
    }
}

object M9TO10 : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Message ADD COLUMN html INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE Message ADD COLUMN date_string TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE Message ADD COLUMN processing_time INTEGER DEFAULT NULL")
        database.execSQL("DELETE FROM Access WHERE username LIKE '%@%'")
    }
}

object M10TO11 : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Class ADD COLUMN partial_score REAL DEFAULT NULL")
    }
}

object M11TO12 : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Access ADD COLUMN valid INTEGER NOT NULL DEFAULT 1")
    }
}

object M12TO13 : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Message ADD COLUMN hash_message INTEGER DEFAULT NULL")
    }
}

object M13TO14 : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Profile ADD COLUMN mocked INTEGER NOT NULL DEFAULT 0")
    }
}

object M14TO15 : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE UNIQUE INDEX `index_Message_hash_message` ON Message (`hash_message`)")
    }
}

object M15TO16 : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE Discipline SET code = TRIM(Discipline.code)")
    }
}

object M16TO17 : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ClassMaterial ADD COLUMN notified INTEGER NOT NULL DEFAULT 1")
    }
}

object M17TO18 : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Course ADD COLUMN description TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE Course ADD COLUMN image TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE Course ADD COLUMN since TEXT DEFAULT NULL")
    }
}

object M18TO19 : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM AccessToken")
    }
}

object M19TO20 : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val table = "Account"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$table` (`id` INTEGER NOT NULL, `name` TEXT, `imageUrl` TEXT, `username` TEXT NOT NULL, `email` TEXT, `darkThemeEnabled` INTEGER NOT NULL, `darkThemeInvites` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}

object M20TO21 : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val teacherServiceTable = "STeacher"
        val disciplineServiceTable = "SDiscipline"
        val studentServiceTable = "SStudent"
        val evaluationEntityTable = "EvaluationEntity"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$teacherServiceTable` (`teacherId` INTEGER NOT NULL, `name` TEXT NOT NULL, `imageUrl` TEXT, PRIMARY KEY(`teacherId`))")
        database.execSQL("CREATE INDEX `index_STeacher_name` ON `$teacherServiceTable` (`name`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `$disciplineServiceTable` (`disciplineId` INTEGER NOT NULL, `department` TEXT NOT NULL, `departmentName` TEXT, `code` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`disciplineId`))")
        database.execSQL("CREATE UNIQUE INDEX `index_SDiscipline_code_department` ON `$disciplineServiceTable` (`code`, `department`)")
        database.execSQL("CREATE INDEX `index_SDiscipline_name` ON `$disciplineServiceTable` (`name`)")
        database.execSQL("CREATE INDEX `index_SDiscipline_code` ON `$disciplineServiceTable` (`code`)")
        database.execSQL("CREATE INDEX `index_SDiscipline_department` ON `$disciplineServiceTable` (`department`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `$studentServiceTable` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `imageUrl` TEXT, `course` INTEGER, `courseName` TEXT, PRIMARY KEY(`id`))")
        database.execSQL("CREATE INDEX `index_SStudent_name` ON `$studentServiceTable` (`name`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `$evaluationEntityTable` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `referencedId` INTEGER NOT NULL, `name` TEXT NOT NULL, `extra` TEXT, `image` TEXT, `type` INTEGER NOT NULL, `searchable` TEXT NOT NULL)")
        database.execSQL("CREATE INDEX `index_EvaluationEntity_name` ON `$evaluationEntityTable` (`name`)")
    }
}

object M21TO22 : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE EvaluationEntity ADD COLUMN comp1 TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE EvaluationEntity ADD COLUMN comp2 TEXT DEFAULT NULL")
    }
}

object M22TO23 : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE SyncRegistry ADD COLUMN skipped INTEGER NOT NULL DEFAULT 0")
    }
}

object M23TO24 : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE SStudent ADD COLUMN me INTEGER NOT NULL DEFAULT 0")
    }
}

object M24TO25 : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val flowchart = "Flowchart"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$flowchart` (`id` INTEGER NOT NULL, `courseId` INTEGER NOT NULL, `description` TEXT NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        val semester = "FlowchartSemester"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$semester` (`id` INTEGER NOT NULL, `flowchartId` INTEGER NOT NULL, `order` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`flowchartId`) REFERENCES `Flowchart`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")
        database.execSQL("CREATE  INDEX `index_FlowchartSemester_flowchartId` ON `$semester` (`flowchartId`)")
        val discipline = "FlowchartDiscipline"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$discipline` (`id` INTEGER NOT NULL, `disciplineId` INTEGER NOT NULL, `type` TEXT NOT NULL, `mandatory` INTEGER NOT NULL, `semesterId` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`disciplineId`) REFERENCES `Discipline`(`uid`) ON UPDATE CASCADE ON DELETE CASCADE , FOREIGN KEY(`semesterId`) REFERENCES `FlowchartSemester`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")
        database.execSQL("CREATE  INDEX `index_FlowchartDiscipline_disciplineId` ON `$discipline` (`disciplineId`)")
        database.execSQL("CREATE  INDEX `index_FlowchartDiscipline_semesterId` ON `$discipline` (`semesterId`)")
    }
}

object M25TO26 : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE FlowchartDiscipline ADD COLUMN completed INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE FlowchartDiscipline ADD COLUMN participating INTEGER NOT NULL DEFAULT 0")
    }
}

object M26TO27 : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val requirements = "FlowchartRequirement"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$requirements` (`id` INTEGER NOT NULL, `type` TEXT NOT NULL, `disciplineId` INTEGER NOT NULL, `requiredDisciplineId` INTEGER, `coursePercentage` REAL, `courseHours` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`disciplineId`) REFERENCES `FlowchartDiscipline`(`id`) ON UPDATE CASCADE ON DELETE CASCADE , FOREIGN KEY(`requiredDisciplineId`) REFERENCES `FlowchartDiscipline`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")
        database.execSQL("CREATE  INDEX `index_FlowchartRequirement_disciplineId` ON `$requirements` (`disciplineId`)")
        database.execSQL("CREATE  INDEX `index_FlowchartRequirement_requiredDisciplineId` ON `$requirements` (`requiredDisciplineId`)")
    }
}

object M27TO28 : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE FlowchartRequirement ADD COLUMN typeId INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE FlowchartRequirement SET typeId = 1 WHERE name = 'Pré Requisitos'")
        database.execSQL("UPDATE FlowchartRequirement SET typeId = 2 WHERE name = 'Co Requisitos'")
    }
}

/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.sagres.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object M1TO2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val tableName = "SPerson"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$tableName` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `exhibitionName` TEXT, `cpf` TEXT, `email` TEXT, `sagres_id` TEXT)")
        database.execSQL("CREATE UNIQUE INDEX `index_SPerson_sagres_id` ON `$tableName` (`sagres_id`)")
        database.execSQL("CREATE  INDEX `index_SPerson_cpf` ON `$tableName` (`cpf`)")
        database.execSQL("CREATE  INDEX `index_SPerson_email` ON `$tableName` (`email`)")
    }
}

object M2TO3 : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val messageScopeTable = "SMessageScope"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$messageScopeTable` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sagres_id` TEXT, `clazz_link` TEXT)")
        database.execSQL("CREATE UNIQUE INDEX `index_SMessageScope_sagres_id` ON `$messageScopeTable` (`sagres_id`)")

        val classTable = "SClass"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$classTable` (`id` INTEGER NOT NULL, `description` TEXT, `kind` TEXT, `link` TEXT, `discipline_link` TEXT, PRIMARY KEY(`id`))")
        database.execSQL("CREATE UNIQUE INDEX `index_SClass_link` ON `$classTable` (`link`)")

        val disciplineResumedTable = "SDisciplineResumed"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$disciplineResumedTable` (`id` INTEGER NOT NULL, `code` TEXT, `name` TEXT, `resumed_name` TEXT, `objective` TEXT, `department_link` TEXT, `link` TEXT, PRIMARY KEY(`id`))")
        database.execSQL("CREATE UNIQUE INDEX `index_SDisciplineResumed_link` ON `$disciplineResumedTable` (`link`)")
    }
}

object M3TO4 : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE SPerson ADD COLUMN mocked INTEGER NOT NULL DEFAULT 0")
    }
}
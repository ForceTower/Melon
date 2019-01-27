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
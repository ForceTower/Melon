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

package com.forcetower.uefs.core.storage.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object M1TO2: Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val sagresFlagsTable = "SagresFlags"
        val demandOfferTable = "SDemandOffer"
        database.execSQL("CREATE TABLE IF NOT EXISTS `$sagresFlagsTable` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `demand_open` INTEGER NOT NULL)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `$demandOfferTable` (`uid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` TEXT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `selected` INTEGER NOT NULL, `category` TEXT NOT NULL, `hours` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `available` INTEGER NOT NULL, `current` INTEGER NOT NULL, `selectable` INTEGER NOT NULL, `unavailable` INTEGER NOT NULL)")
        database.execSQL("INSERT INTO `$sagresFlagsTable` (`uid`, `demand_open`) VALUES (1, 0)")
    }
}
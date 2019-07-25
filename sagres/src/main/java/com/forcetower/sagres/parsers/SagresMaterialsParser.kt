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

package com.forcetower.sagres.parsers

import com.forcetower.sagres.database.model.SMaterialLink
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber

object SagresMaterialsParser {
    @JvmStatic
    fun extractMaterials(document: Document): List<SMaterialLink> {
        val materials = mutableListOf<SMaterialLink>()
        var elements = document.select("label[class=\"material_apoio_arquivo\"]")
        for (element in elements) {
            elementProcessing(element, materials)
        }

        elements = document.select("label[class=\"material_apoio_url\"]")
        for (element in elements) {
            elementProcessing(element, materials)
        }

        elements = document.select("label[class=\"material_apoio_aula\"]")
        for (element in elements) {
            elementProcessing(element, materials)
        }

        elements = document.select("label[class=\"material_apoio_grid_aula\"]")
        for (element in elements) {
            elementProcessing(element, materials)
        }
        return materials
    }

    @JvmStatic
    private fun elementProcessing(element: Element, materials: MutableList<SMaterialLink>) {
        val a = element.selectFirst("a") ?: return

        val link = if (a.attr("href").isEmpty()) a.attr("href") else a.attr("HREF")
        val name = element.parent()?.parent()?.parent()?.parent()?.parent()?.parent()?.selectFirst("td")?.text() ?: "Arquivo"
        Timber.d("Defined new material $name at $link")

        materials.add(SMaterialLink(name, link))
    }
}
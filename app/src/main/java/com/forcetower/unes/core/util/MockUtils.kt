/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.forcetower.unes.core.util

import com.forcetower.unes.core.model.Contributor

object MockUtils {

    @JvmStatic
    fun contributors(): List<Contributor> {
        val c1 = Contributor(1, "João Paulo Sena", "Programação do núcleo", "https://avatars.githubusercontent.com/ForceTower", "http://facebook.com/ForceTower")
        val c2 = Contributor(2, "Alberto Junior", "Muitas Idéias Geniais", "https://avatars.githubusercontent.com/AlbertoJunior", "http://facebook.com/alberto.junior.995")
        val c3 = Contributor(3, "Galuber Silva", "Tradução dos Textos (en)", "https://avatars.githubusercontent.com/sglauber", "http://facebook.com/Gss.14")
        return listOf(c1, c2, c3)
    }

}

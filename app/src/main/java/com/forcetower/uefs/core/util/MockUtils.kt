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

package com.forcetower.uefs.core.util

import com.forcetower.uefs.core.model.unes.Contributor

object MockUtils {

    @JvmStatic
    fun contributors(): List<Contributor> {
        val c1 = Contributor(1, "Lokisley \"Lokisssss\" Oliveira", "Icone do Aplicativo", "https://avatars.githubusercontent.com/Lokisley", "https://www.facebook.com/Lokisley")
        val c2 = Contributor(2, "Marcus \"Kuchuki\" Aldrey", "Jogador de Gnar", "https://avatars.githubusercontent.com/MarcusAldrey", "https://www.facebook.com/marcus.aldrey")
        val c3 = Contributor(3, "Matheus Teixeira", "Nome do Aplicativo", "https://i.imgur.com/VikWE5N.jpg", "https://www.facebook.com/teixeirista")
        val c4 = Contributor(4, "Alberto \"Da Pesada\" Junior", "Muitas idéias geniais", "https://avatars.githubusercontent.com/AlbertoJunior", "http://facebook.com/alberto.junior.995")
        val c5 = Contributor(5, "Emerson Souza", "Muitas idéias geniais", "https://avatars.githubusercontent.com/EmersonBrSouza", "https://www.facebook.com/emerson.souza.fsa")
        val c6 = Contributor(6, "Galuber Silva", "Tradução dos Textos (en)", "https://avatars.githubusercontent.com/sglauber", "http://facebook.com/Gss.14")
        return listOf(c1, c2, c3, c4, c5, c6)
    }
}

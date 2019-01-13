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

object Config {
    const val applicationId = "com.forcetower.uefs"

    fun buildVersionName(version: String): String {
        val countString = Runtime.getRuntime().exec("git rev-list --count HEAD").inputStream.reader().use { it.readText() }.trim();
        val commitCount = Integer.parseInt(countString)
        val definitions = version.toLowerCase().replace("-", "").split(".")

        val postfix = when {
            definitions[2].endsWith("dev") -> "[development]"
            definitions[2].endsWith("pr") -> "[pre-release]"
            else -> "[release]"
        }

        val regex = Regex("[^0-9]")

        val major = Integer.parseInt(definitions[0])
        val minor = Integer.parseInt(definitions[1])
        val patch = Integer.parseInt(regex.replace(definitions[2], ""))

        return "$major.$minor.$patch build $commitCount $postfix"
    }

    fun buildVersionCode(): Int {
        val countString = Runtime.getRuntime().exec("git rev-list --count HEAD").inputStream.reader().use { it.readText() }.trim();
        val commitCount = Integer.parseInt(countString)
        val code = (21 * 100000) + commitCount
        if (code > 2500000) throw RuntimeException("Flying too close to the sun. Project is over 400k commits. Aborting...")
        return code
    }
}
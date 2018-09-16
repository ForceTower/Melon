package com.forcetower.uefs.feature.shared

fun String.makeSemester(): String {
    return if (this.length > 4) {
        this.substring(0, 4) + "." + this.substring(4)
    } else {
        this
    }
}
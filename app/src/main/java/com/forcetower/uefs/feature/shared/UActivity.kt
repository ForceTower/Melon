package com.forcetower.uefs.feature.shared

import androidx.appcompat.app.AppCompatActivity

abstract class UActivity : AppCompatActivity() {

    open fun showSnack(string: String) {}
}
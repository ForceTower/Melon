package com.forcetower.uefs.feature.flowchart

import android.widget.TextView
import androidx.databinding.BindingAdapter

@BindingAdapter("departmentStrip")
fun departmentStrip(tv: TextView, department: String?) {
    department ?: return
    tv.text = department.replace("departamento de ", "", ignoreCase = true)
}
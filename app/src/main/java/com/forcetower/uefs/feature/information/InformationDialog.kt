package com.forcetower.uefs.feature.information

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.DialogInformationBaseBinding
import com.forcetower.uefs.feature.shared.RoundedDialog

class InformationDialog : RoundedDialog() {
    private lateinit var binding: DialogInformationBaseBinding
    lateinit var title: String
    lateinit var description: String
    @Suppress("MemberVisibilityCanBePrivate")
    var buttonText: String? = null

    override fun onChildCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogInformationBaseBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            btnOk.setOnClickListener { dismiss() }
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title = title
        binding.description = description
        binding.buttonText = buttonText ?: getString(R.string.got_it)
    }
}
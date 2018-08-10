package com.forcetower.unes.feature.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.unes.R
import com.forcetower.unes.feature.shared.UFragment

class SagresMessagesFragment: UFragment() {
    init { displayName = "Sagres" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sagres_messages, container, false)
    }
}
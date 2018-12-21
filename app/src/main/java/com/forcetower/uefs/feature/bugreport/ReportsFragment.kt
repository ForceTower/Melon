package com.forcetower.uefs.feature.bugreport

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.feature.shared.UFragment

class ReportsFragment : UFragment(), Injectable {

    // TODO Make this an activity with a bottom app bar... I want to test this :)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}
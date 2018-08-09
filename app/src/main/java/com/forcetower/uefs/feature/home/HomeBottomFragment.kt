package com.forcetower.uefs.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.forcetower.uefs.R
import com.forcetower.uefs.databinding.HomeBottomBinding
import com.forcetower.uefs.feature.shared.RoundedBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_home_bottom_sheet.*
import timber.log.Timber

class HomeBottomFragment : RoundedBottomSheetDialogFragment() {
    private lateinit var binding: HomeBottomBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return HomeBottomBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        navigation_view.setNavigationItemSelectedListener{item ->
            when (item.itemId) {
                R.id.messages -> Timber.d("Messages")
                R.id.grades_disciplines -> Timber.d("Grades")
            }
            dismiss()
            true
        }
    }
}
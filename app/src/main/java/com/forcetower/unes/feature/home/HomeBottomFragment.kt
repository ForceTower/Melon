package com.forcetower.unes.feature.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.forcetower.sagres.database.model.Message
import com.forcetower.unes.R
import com.forcetower.unes.databinding.HomeBottomBinding
import com.forcetower.unes.feature.shared.RoundedBottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_home_bottom_sheet.*
import timber.log.Timber

class HomeBottomFragment : RoundedBottomSheetDialogFragment() {
    private lateinit var binding: HomeBottomBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return HomeBottomBinding.inflate(inflater, container, false).also {
            binding = it
        }.root.also {  }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        navigation_view.setNavigationItemSelectedListener{item ->
            when (item.itemId) {
                R.id.messages -> activity?.findNavController(R.id.home_nav_host)?.navigate(R.id.messages)
                R.id.grades_disciplines -> Timber.d("Grades")
            }
            dismiss()
            true
        }
    }
}
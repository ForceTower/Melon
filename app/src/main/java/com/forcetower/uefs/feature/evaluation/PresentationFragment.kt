package com.forcetower.uefs.feature.evaluation

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.databinding.FragmentEvaluationPresentationBinding
import com.forcetower.uefs.feature.information.InformationDialog
import com.forcetower.uefs.feature.shared.UFragment
import javax.inject.Inject

class PresentationFragment : UFragment(), Injectable {
    @Inject
    lateinit var preferences: SharedPreferences
    private lateinit var binding: FragmentEvaluationPresentationBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentEvaluationPresentationBinding.inflate(inflater, container, false).also {
            binding = it
        }.apply {
            btnConnect.setOnClickListener { next() }
            btnQuestion.setOnClickListener { onQuestion() }
        }.root
    }

    fun next() {
        val direction = PresentationFragmentDirections.actionPresentationToHome()
        preferences.edit().putBoolean("evaluation_presentation_shown", true).apply()
        findNavController().navigate(direction)
    }

    private fun onQuestion() {
        val dialog = InformationDialog()
        dialog.title = getString(R.string.evaluation_what_is)
        dialog.description = getString(R.string.evaluation_what_is_description)
        dialog.show(childFragmentManager, "what_is_evaluation")
    }
}
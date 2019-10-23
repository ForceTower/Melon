package com.forcetower.uefs.feature.forms

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Question
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.ActivityFormsBinding
import com.forcetower.uefs.feature.shared.FragmentAdapter
import com.forcetower.uefs.feature.shared.UActivity
import com.forcetower.uefs.feature.shared.extensions.provideViewModel
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class FormActivity : UActivity(), HasAndroidInjector {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Any>

    private lateinit var binding: ActivityFormsBinding
    private lateinit var viewModel: FormsViewModel
    private lateinit var adapter: FragmentAdapter
    private lateinit var currentData: List<Question>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_forms)
        viewModel = provideViewModel(factory)
        adapter = FragmentAdapter(supportFragmentManager)
        binding.viewPager.adapter = adapter

        val questions = createQuestions()
        currentData = questions
        createFragmentsList(questions)

        viewModel.nextQuestion.observe(this, EventObserver {
            val position = binding.viewPager.currentItem
            val size = currentData.size
            val nextPos = position + 1
            if (nextPos >= size) {
                viewModel.submitAnswers()
                finish()
            } else {
                binding.viewPager.setCurrentItem(nextPos, true)
            }
        })
    }

    private fun createQuestions(): List<Question> {
        return listOf(
            Question(
                0,
                "O quão satisfeito você está com o Aplicativo UNES?",
                "Onde 1 significa pouco satisfeito e 5 significa muito satisfeito",
                teacher = false,
                discipline = false,
                formId = "entry.729050027"
            ),
            Question(
                1,
                "O quanto você está incomodado com a forma como o Aplicativo UNES exibe os anúncios atualmente?",
                "Onde 1 significa não incomoda e 5 significa incomoda muito",
                teacher = false,
                discipline = false,
                formId = "entry.611283944"
            ),
            Question(
                2,
                "O quanto você recomendaria o Aplicativo UNES para um colega?",
                "Onde 1 significa de jeito nenhum e 5 com certeza recomendo",
                teacher = false,
                discipline = false,
                formId = "entry.637152166"
            )
        )
    }

    private fun createFragmentsList(data: List<Question>) {
        val fragments = data.map { InternalFormFragment.newInstance(it) }
        adapter.setItems(fragments)
    }

    override fun androidInjector() = fragmentInjector
}
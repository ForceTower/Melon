package com.forcetower.unes.feature.disciplines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.core.injection.Injectable
import com.forcetower.unes.core.model.unes.Semester
import com.forcetower.unes.core.vm.DisciplineViewModel
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.databinding.FragmentDisciplineSemesterBinding
import com.forcetower.unes.feature.shared.UFragment
import com.forcetower.unes.feature.shared.provideActivityViewModel
import javax.inject.Inject

class DisciplineSemesterFragment: UFragment(), Injectable {

    companion object {
        const val SEMESTER_SAGRES_ID = "unes_sagres_id"
        const val SEMESTER_DATABASE_ID = "unes_database_id"

        fun newInstance(semester: Semester): DisciplineSemesterFragment {
            val args = bundleOf(SEMESTER_SAGRES_ID to semester.sagresId, SEMESTER_DATABASE_ID to semester.uid)
            return DisciplineSemesterFragment().apply { arguments = args }
        }
    }

    @Inject
    lateinit var factory: UViewModelFactory
    private lateinit var viewModel: DisciplineViewModel
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel = provideActivityViewModel(factory)
        val binding = FragmentDisciplineSemesterBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@DisciplineSemesterFragment)
            viewModel = this@DisciplineSemesterFragment.viewModel
        }.also {
            recyclerView = it.disciplinesRecycler
        }

        return binding.root
    }
}
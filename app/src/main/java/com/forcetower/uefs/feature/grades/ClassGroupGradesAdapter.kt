package com.forcetower.uefs.feature.grades

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.Grade
import com.forcetower.uefs.databinding.ItemGradeBinding
import com.forcetower.uefs.feature.shared.inflater

private const val GRADE = 3

class ClassGroupGradesAdapter: ListAdapter<Grade, GradesHolder>(GradesDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradesHolder {
        val binding = ItemGradeBinding.inflate(parent.inflater(), parent, false)
        return GradesHolder(binding)
    }

    override fun onBindViewHolder(holder: GradesHolder, position: Int) = holder.bind(getItem(position))
    override fun getItemViewType(position: Int) = GRADE
}

class GradesHolder(
    val binding: ItemGradeBinding
): RecyclerView.ViewHolder(binding.root) {
    fun bind(grade: Grade) {
        binding.grade = grade
        binding.executePendingBindings()
    }
}

object GradesDiff: DiffUtil.ItemCallback<Grade>() {
    override fun areItemsTheSame(oldItem: Grade, newItem: Grade) = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: Grade, newItem: Grade) = oldItem == newItem

}
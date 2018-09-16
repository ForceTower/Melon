package com.forcetower.uefs.feature.disciplines

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.storage.database.accessors.ClassWithGroups
import com.forcetower.uefs.core.vm.DisciplineViewModel
import com.forcetower.uefs.databinding.ItemDisciplineCollapsedBinding
import com.forcetower.uefs.feature.shared.inflater

private const val DISCIPLINE: Int = 0
private const val GRADE: Int = 1

class DisciplineSemesterAdapter (
    val viewModel: DisciplineViewModel
): ListAdapter<ClassWithGroups, ClassHolder>(ClassDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassHolder {
        val binding = ItemDisciplineCollapsedBinding.inflate(parent.inflater(), parent, false)
        return ClassHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassHolder, position: Int) = holder.bind(getItem(position))
}

class ClassHolder(
    val binding: ItemDisciplineCollapsedBinding
): RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ClassWithGroups) {
        binding.clazzGroup = item
        binding.executePendingBindings()
    }
}

object ClassDiff: DiffUtil.ItemCallback<ClassWithGroups>() {
    override fun areItemsTheSame(oldItem: ClassWithGroups, newItem: ClassWithGroups) = oldItem.clazz.uid == newItem.clazz.uid
    override fun areContentsTheSame(oldItem: ClassWithGroups, newItem: ClassWithGroups) = oldItem == newItem
}



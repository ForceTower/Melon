package com.forcetower.uefs.feature.flowchart.semester

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.databinding.ItemFlowchartDisciplineBinding
import com.forcetower.uefs.feature.shared.inflate

class DisciplinesAdapter(
    private val interactor: DisciplineInteractor
) : ListAdapter<FlowchartDisciplineUI, DisciplinesAdapter.DisciplineHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplineHolder {
        return DisciplineHolder(parent.inflate(R.layout.item_flowchart_discipline), interactor)
    }

    override fun onBindViewHolder(holder: DisciplineHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            discipline = item
            executePendingBindings()
        }
    }

    class DisciplineHolder(val binding: ItemFlowchartDisciplineBinding, interactor: DisciplineInteractor) : RecyclerView.ViewHolder(binding.root) {
        init { binding.interactor = interactor }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FlowchartDisciplineUI>() {
        override fun areItemsTheSame(oldItem: FlowchartDisciplineUI, newItem: FlowchartDisciplineUI): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FlowchartDisciplineUI, newItem: FlowchartDisciplineUI): Boolean {
            return oldItem == newItem
        }
    }
}

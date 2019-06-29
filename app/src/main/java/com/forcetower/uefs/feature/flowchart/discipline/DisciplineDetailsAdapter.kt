package com.forcetower.uefs.feature.flowchart.discipline

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.FlowchartDisciplineUI
import com.forcetower.uefs.databinding.ItemFlowchartDisciplineGroupingBinding
import com.forcetower.uefs.databinding.ItemFlowchartDisciplineMinifiedBinding
import com.forcetower.uefs.feature.flowchart.semester.DisciplineInteractor
import com.forcetower.uefs.feature.shared.inflate

class DisciplineDetailsAdapter(
    private val interactor: DisciplineInteractor
) : RecyclerView.Adapter<DisciplineDetailsAdapter.DisciplineDetailsHolder>() {
    private val differ = AsyncListDiffer(this, DiffCallback)
    var currentList: List<FlowchartDisciplineUI> = listOf()
        set(value) {
            field = value
            differ.submitList(buildMergedList(value))
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisciplineDetailsHolder {
        return when (viewType) {
            R.layout.item_flowchart_discipline_grouping -> DisciplineDetailsHolder.CategoryHolder(parent.inflate(viewType))
            R.layout.item_flowchart_discipline_minified -> DisciplineDetailsHolder.DisciplineHolder(parent.inflate(viewType), interactor)
            else -> throw IllegalStateException("No view defined for view type $viewType")
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: DisciplineDetailsHolder, position: Int) {
        val item = differ.currentList[position]
        when (holder) {
            is DisciplineDetailsHolder.CategoryHolder -> {
                holder.binding.apply {
                    name = (item as DisciplineTitle).title
                    executePendingBindings()
                }
            }
            is DisciplineDetailsHolder.DisciplineHolder -> {
                holder.binding.apply {
                    discipline = item as FlowchartDisciplineUI
                    executePendingBindings()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            is DisciplineTitle -> R.layout.item_flowchart_discipline_grouping
            is FlowchartDisciplineUI -> R.layout.item_flowchart_discipline_minified
            else -> throw IllegalStateException("No view defined for position $position item $item")
        }
    }

    private fun buildMergedList(list: List<FlowchartDisciplineUI>): List<Any> {
        val result = mutableListOf<Any>()
        val mapped = list.groupBy { it.type }
        mapped.entries.forEach {
            result += DisciplineTitle(it.key)
            result.addAll(it.value)
        }
        return result
    }

    private data class DisciplineTitle(
        val title: String
    )

    sealed class DisciplineDetailsHolder(view: View) : RecyclerView.ViewHolder(view) {
        class CategoryHolder(val binding: ItemFlowchartDisciplineGroupingBinding) : DisciplineDetailsHolder(binding.root)
        class DisciplineHolder(val binding: ItemFlowchartDisciplineMinifiedBinding, interactor: DisciplineInteractor) : DisciplineDetailsHolder(binding.root) {
            init { binding.interactor = interactor }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is DisciplineTitle && newItem is DisciplineTitle -> oldItem == newItem
                oldItem is FlowchartDisciplineUI && newItem is FlowchartDisciplineUI -> oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is DisciplineTitle && newItem is DisciplineTitle -> oldItem == newItem
                oldItem is FlowchartDisciplineUI && newItem is FlowchartDisciplineUI -> oldItem == newItem
                else -> true
            }
        }
    }
}

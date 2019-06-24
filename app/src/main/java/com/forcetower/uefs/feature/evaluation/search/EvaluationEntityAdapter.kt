package com.forcetower.uefs.feature.evaluation.search

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.EvaluationEntity
import com.forcetower.uefs.databinding.ItemEvaluationSimpleEntityBinding
import com.forcetower.uefs.feature.shared.inflate

class EvaluationEntityAdapter(
    private val selector: EntitySelector
) : PagedListAdapter<EvaluationEntity, EvaluationEntityAdapter.EntityHolder>(EntityDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = EntityHolder(
        parent.inflate(R.layout.item_evaluation_simple_entity),
        selector
    )

    override fun onBindViewHolder(holder: EntityHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            entity = item
            executePendingBindings()
        }
    }

    class EntityHolder(
        val binding: ItemEvaluationSimpleEntityBinding,
        selector: EntitySelector
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.interactor = selector
        }
    }
}

private object EntityDiff : DiffUtil.ItemCallback<EvaluationEntity>() {
    override fun areItemsTheSame(oldItem: EvaluationEntity, newItem: EvaluationEntity) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: EvaluationEntity, newItem: EvaluationEntity) = oldItem == newItem
}

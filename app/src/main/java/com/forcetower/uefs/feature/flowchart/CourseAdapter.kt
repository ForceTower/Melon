package com.forcetower.uefs.feature.flowchart

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.Flowchart
import com.forcetower.uefs.databinding.ItemFlowchartCourseBinding
import com.forcetower.uefs.feature.shared.inflate

class CourseAdapter(
    private val interactor: FlowchartInteractor
) : ListAdapter<Flowchart, CourseAdapter.FlowchartHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FlowchartHolder(
        parent.inflate(R.layout.item_flowchart_course), interactor
    )

    override fun onBindViewHolder(holder: FlowchartHolder, position: Int) {
        val item = getItem(position)
        holder.binding.flowchart = item
    }

    class FlowchartHolder(
        val binding: ItemFlowchartCourseBinding,
        interactor: FlowchartInteractor
    ) : RecyclerView.ViewHolder(binding.root) {
        init { binding.interactor = interactor }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Flowchart>() {
        override fun areItemsTheSame(oldItem: Flowchart, newItem: Flowchart) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Flowchart, newItem: Flowchart) = oldItem == newItem
    }
}

package com.forcetower.unes.feature.messages

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.unes.core.model.sagres.SagresMessage
import com.forcetower.unes.databinding.ItemSagresMessageBinding
import com.forcetower.unes.feature.shared.inflater

class SagresMessageAdapter(diffCallback: DiffUtil.ItemCallback<SagresMessage>):
        PagedListAdapter<SagresMessage, SagresMessageAdapter.MessageHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        val binding = ItemSagresMessageBinding.inflate(parent.inflater(), parent, false)
        return MessageHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageHolder(val binding: ItemSagresMessageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: SagresMessage?) {
            TODO("Implement Message Binding")
        }
    }
}
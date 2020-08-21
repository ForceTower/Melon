/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.document

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.databinding.ItemDocumentBinding
import com.forcetower.uefs.databinding.ItemDocumentHeaderBinding
import com.forcetower.uefs.feature.shared.inflater

class DocumentsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: DocumentsViewModel
) : RecyclerView.Adapter<DocumentViewHolder>() {
    var documents: List<SagresDocument> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList())
        }

    private val differ = AsyncListDiffer(this, DocumentDiff)
    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val inflater = parent.inflater()
        return when (viewType) {
            R.layout.item_document -> DocumentViewHolder.DocumentHolder(
                ItemDocumentBinding.inflate(inflater, parent, false).apply {
                    listener = viewModel
                    lifecycleOwner = this@DocumentsAdapter.lifecycleOwner
                }
            )
            R.layout.item_document_header -> DocumentViewHolder.HeaderHolder(
                ItemDocumentHeaderBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        when (holder) {
            is DocumentViewHolder.DocumentHolder -> holder.bind(differ.currentList[position] as SagresDocument)
            is DocumentViewHolder.HeaderHolder -> Unit
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is DocumentHeader -> R.layout.item_document_header
            is SagresDocument -> R.layout.item_document
            else -> throw IllegalStateException("Unknown view type at position $position")
        }
    }

    private fun buildMergedList(
        list: List<SagresDocument> = documents
    ): List<Any> {
        val merged = mutableListOf<Any>(DocumentHeader)
        if (list.isNotEmpty()) {
            merged.addAll(list)
        }
        return merged
    }
}

sealed class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    class DocumentHolder(
        private val binding: ItemDocumentBinding
    ) : DocumentViewHolder(binding.root) {

        fun bind(document: SagresDocument) {
            binding.document = document
            binding.executePendingBindings()
        }
    }

    class HeaderHolder(
        binding: ItemDocumentHeaderBinding
    ) : DocumentViewHolder(binding.root)
}

object DocumentHeader

object DocumentDiff : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === DocumentHeader && newItem === DocumentHeader -> true
            oldItem is SagresDocument && newItem is SagresDocument -> oldItem.uid == newItem.uid
            else -> false
        }
    }
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is SagresDocument && newItem is SagresDocument -> oldItem == newItem
            else -> true
        }
    }
}

/*
 * Copyright (c) 2019.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

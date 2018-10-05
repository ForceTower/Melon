/*
 * Copyright (c) 2018.
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

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.core.model.unes.SagresDocument
import com.forcetower.uefs.databinding.ItemDocumentBinding
import com.forcetower.uefs.feature.shared.inflater

class DocumentsAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: DocumentsViewModel
): ListAdapter<SagresDocument, DocumentHolder>(DocumentDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentHolder {
        val binding = ItemDocumentBinding.inflate(parent.inflater(), parent, false).apply {
            setLifecycleOwner(lifecycleOwner)
            listener = viewModel
        }
        return DocumentHolder(binding)
    }

    override fun onBindViewHolder(holder: DocumentHolder, position: Int) = holder.bind(getItem(position))
}

class DocumentHolder(
    private val binding: ItemDocumentBinding
): RecyclerView.ViewHolder(binding.root) {

    fun bind(document: SagresDocument) {
        binding.document = document
        binding.executePendingBindings()
    }
}

object DocumentDiff: DiffUtil.ItemCallback<SagresDocument>() {
    override fun areItemsTheSame(oldItem: SagresDocument, newItem: SagresDocument) = oldItem.uid == newItem.uid
    override fun areContentsTheSame(oldItem: SagresDocument, newItem: SagresDocument) = oldItem == newItem
}


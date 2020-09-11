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

package com.forcetower.uefs.feature.profile

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.core.adapters.ImageLoadListener
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ProfileStatement
import com.forcetower.uefs.databinding.ItemProfileHeaderBinding
import com.forcetower.uefs.databinding.ItemProfileStatementBinding
import com.forcetower.uefs.databinding.ItemProfileStatementHeaderBinding
import com.forcetower.uefs.databinding.ItemProfileStatementUnapprovedHeaderBinding
import com.forcetower.uefs.feature.shared.inflate

class ProfileAdapter(
    private val viewModel: ProfileViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val headLoadListener: ImageLoadListener,
    private val interactor: ProfileInteractor? = null
) : RecyclerView.Adapter<ProfileAdapter.ProfileHolder>() {
    private val differ = AsyncListDiffer(this, DiffCallback)
    var statements = emptyList<ProfileStatement>()
        set(value) {
            field = value
            differ.submitList(buildMergedList(stats = value))
        }

    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
        return when (viewType) {
            R.layout.item_profile_header -> ProfileHolder.Header(parent.inflate(viewType), interactor)
            R.layout.item_profile_statement_header -> ProfileHolder.StatementHeader(parent.inflate(viewType))
            R.layout.item_profile_statement -> ProfileHolder.Statement(parent.inflate(viewType), interactor, lifecycleOwner)
            R.layout.item_profile_statement_unapproved_header -> ProfileHolder.StatementUnapprovedHeader(parent.inflate(viewType))
            else -> throw IllegalStateException("No view matching view type $viewType")
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
        val item = differ.currentList[position]
        when (holder) {
            is ProfileHolder.Header -> {
                holder.binding.apply {
                    account = viewModel.profile
                    headshotImageListener = headLoadListener
                    lifecycleOwner = this@ProfileAdapter.lifecycleOwner
                }
            }
            is ProfileHolder.Statement -> {
                holder.binding.apply {
                    account = viewModel.account
                    statement = item as ProfileStatement
                    executePendingBindings()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            is ProfileHeader -> R.layout.item_profile_header
            is StatementsHeader -> R.layout.item_profile_statement_header
            is ProfileStatement -> R.layout.item_profile_statement
            is UnapprovedStatementsHeader -> R.layout.item_profile_statement_unapproved_header
            else -> throw IllegalStateException("Can't find a view type for item $item")
        }
    }

    private fun buildMergedList(
        stats: List<ProfileStatement> = statements
    ): List<Any> {
        val merged = mutableListOf<Any>()
        merged += ProfileHeader
        if (stats.isNotEmpty()) {
            val approved = stats.filter { it.approved }
            val unapproved = stats.filter { !it.approved }
            if (unapproved.isNotEmpty()) {
                merged += UnapprovedStatementsHeader
                merged.addAll(unapproved)
            }
            if (approved.isNotEmpty()) {
                merged += StatementsHeader
                merged.addAll(approved)
            }
        }
        return merged
    }

    sealed class ProfileHolder(view: View) : RecyclerView.ViewHolder(view) {
        class Header(val binding: ItemProfileHeaderBinding, interactor: ProfileInteractor?) : ProfileHolder(binding.root) {
            init { binding.interactor = interactor }
        }
        class Statement(
            val binding: ItemProfileStatementBinding,
            interactor: ProfileInteractor?,
            lifecycleOwner: LifecycleOwner
        ) : ProfileHolder(binding.root) {
            init {
                binding.interactor = interactor
                binding.lifecycleOwner = lifecycleOwner
            }
        }
        class StatementHeader(binding: ItemProfileStatementHeaderBinding) : ProfileHolder(binding.root)
        class StatementUnapprovedHeader(binding: ItemProfileStatementUnapprovedHeaderBinding) : ProfileHolder(binding.root)
    }

    private object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ProfileStatement && newItem is ProfileStatement -> oldItem.id == newItem.id
                oldItem is ProfileHeader && newItem is ProfileHeader -> true
                oldItem is StatementsHeader && newItem is StatementsHeader -> true
                oldItem is UnapprovedStatementsHeader && newItem is UnapprovedStatementsHeader -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ProfileStatement && newItem is ProfileStatement -> oldItem == newItem
                else -> true
            }
        }
    }

    private object ProfileHeader
    private object StatementsHeader
    private object UnapprovedStatementsHeader
}

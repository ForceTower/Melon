package com.forcetower.uefs.feature.profile

import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.model.unes.ProfileStatement
import com.forcetower.uefs.databinding.ItemProfileHeaderBinding
import com.forcetower.uefs.databinding.ItemProfileStatementHeaderBinding
import com.forcetower.uefs.feature.shared.inflate
import com.forcetower.uefs.feature.siecomp.speaker.ImageLoadListener

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
            else -> throw IllegalStateException("No view matching view type $viewType")
        }
    }

    override fun getItemCount() = differ.currentList.size

    override fun onBindViewHolder(holder: ProfileHolder, position: Int) {
//        val item = differ.currentList[position]
        when (holder) {
            is ProfileHolder.Header -> {
                holder.binding.apply {
                    account = viewModel.profile
                    headshotImageListener = headLoadListener
                    lifecycleOwner = this@ProfileAdapter.lifecycleOwner
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = differ.currentList[position]) {
            is ProfileHeader -> R.layout.item_profile_header
            is StatementsHeader -> R.layout.item_profile_statement_header
            is ProfileStatement -> R.layout.item_profile_statement
            else -> throw IllegalStateException("Can't find a view type for item $item")
        }
    }

    private fun buildMergedList(
        stats: List<ProfileStatement> = statements
    ): List<Any> {
        val merged = mutableListOf<Any>()
        merged += ProfileHeader
        if (stats.isNotEmpty()) {
            merged += StatementsHeader
            merged.addAll(stats)
        }
        return merged
    }

    sealed class ProfileHolder(view: View) : RecyclerView.ViewHolder(view) {
        class Header(val binding: ItemProfileHeaderBinding, interactor: ProfileInteractor?) : ProfileHolder(binding.root) {
            init { binding.interactor = interactor }
        }
        class Statement(view: View, interactor: ProfileInteractor?) : ProfileHolder(view) {
            init { }
        }
        class StatementHeader(binding: ItemProfileStatementHeaderBinding) : ProfileHolder(binding.root)
    }

    private object DiffCallback : DiffUtil.ItemCallback<Any>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            return when {
                oldItem is ProfileStatement && newItem is ProfileStatement -> oldItem.id == newItem.id
                oldItem is ProfileHeader && newItem is ProfileHeader -> true
                oldItem is StatementsHeader && newItem is StatementsHeader -> true
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
}

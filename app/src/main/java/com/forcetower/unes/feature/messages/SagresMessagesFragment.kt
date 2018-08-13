package com.forcetower.unes.feature.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedList
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.forcetower.unes.R
import com.forcetower.unes.core.model.Message
import com.forcetower.unes.core.vm.UViewModelFactory
import com.forcetower.unes.feature.shared.UFragment
import kotlinx.android.synthetic.main.fragment_unes_messages.*
import javax.inject.Inject

class SagresMessagesFragment: UFragment() {
    @Inject
    lateinit var vmFactory: UViewModelFactory

    init { displayName = "Sagres" }

    private val adapter by lazy { SagresMessageAdapter(diffCallback = object: DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem.sagresId == newItem.sagresId
        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean = oldItem == newItem
    })}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =  inflater.inflate(R.layout.fragment_sagres_messages, container, false)
        setupRecycler()
        return view
    }

    private fun setupRecycler() {
        recycler_sagres_messages.adapter = adapter
        recycler_sagres_messages.layoutManager = LinearLayoutManager(context)
        recycler_sagres_messages.itemAnimator = DefaultItemAnimator()
        recycler_sagres_messages.isNestedScrollingEnabled = false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    private fun onMessagesChange(list: PagedList<Message>) {
        adapter.submitList(list)
    }
}
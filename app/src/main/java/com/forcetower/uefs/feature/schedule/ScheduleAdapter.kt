/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
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

package com.forcetower.uefs.feature.schedule

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.forcetower.uefs.R
import com.forcetower.uefs.core.storage.database.accessors.LocationWithGroup
import com.forcetower.core.utils.ColorUtils
import com.forcetower.uefs.databinding.ItemScheduleBlockClassBinding
import com.forcetower.uefs.databinding.ItemScheduleBlockHeadNotBinding
import com.forcetower.uefs.databinding.ItemScheduleBlockHeaderBinding
import com.forcetower.uefs.databinding.ItemScheduleBlockNothingBinding
import com.forcetower.uefs.databinding.ItemScheduleBlockTimeBinding
import com.forcetower.uefs.databinding.ItemScheduleDayBinding
import com.forcetower.uefs.databinding.ItemScheduleLineClassBinding
import com.forcetower.uefs.databinding.ItemScheduleLineDayBinding
import com.forcetower.uefs.feature.shared.inflater
import com.forcetower.uefs.feature.shared.extensions.positionOf
import com.forcetower.uefs.feature.shared.extensions.toLongWeekDay
import com.forcetower.uefs.feature.shared.extensions.toWeekDay
import java.util.HashMap

// ---------------------------------------- Schedule Line -------------------------------------------

private const val SCHEDULE_LINE_DAY_HOLDER: Int = 1
private const val SCHEDULE_LINE_CLASS_HOLDER: Int = 2

class ScheduleLineAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: ScheduleViewModel,
    private val pool: RecyclerView.RecycledViewPool
) : ListAdapter<ScheduleDay, DayLineHolder>(DayLineDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayLineHolder {
        val binding = ItemScheduleLineDayBinding.inflate(parent.inflater(), parent, false)
        return DayLineHolder(binding, pool, viewModel, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: DayLineHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int) = SCHEDULE_LINE_DAY_HOLDER

    fun adaptList(locations: List<LocationWithGroup>) {
        val map = locations.groupBy { it.location.day.trim() }
        val list = ArrayList<ScheduleDay>()

        for (i in 1..7) {
            val day = i.toWeekDay()
            val longDay = i.toLongWeekDay()
            val classes = map[day]
            if (classes != null) list.add(ScheduleDay(longDay, classes))
        }

        submitList(list)
    }
}

class ScheduleLineClassAdapter(
    private val viewModel: ScheduleViewModel,
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<LocationWithGroup, LocationLineHolder>(ClassLineDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationLineHolder {
        val binding = ItemScheduleLineClassBinding.inflate(parent.inflater(), parent, false)
        return LocationLineHolder(binding, viewModel, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: LocationLineHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int) = SCHEDULE_LINE_CLASS_HOLDER
}

class DayLineHolder(
    private val binding: ItemScheduleLineDayBinding,
    pool: RecyclerView.RecycledViewPool,
    private val viewModel: ScheduleViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.ViewHolder(binding.root) {
    private val adapter by lazy { ScheduleLineClassAdapter(viewModel, lifecycleOwner) }
    init {
        binding.recyclerDay.setRecycledViewPool(pool)
        binding.recyclerDay.adapter = adapter
        binding.recyclerDay.itemAnimator = DefaultItemAnimator()
    }

    fun bind(day: ScheduleDay) {
        binding.textScheduleDay.text = day.day
        adapter.submitList(day.location)
    }
}

class LocationLineHolder(
    private val binding: ItemScheduleLineClassBinding,
    private val viewModel: ScheduleViewModel,
    private val lifecycleOwner: LifecycleOwner
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(locationWithGroup: LocationWithGroup) {
        binding.run {
            location = locationWithGroup
            interactor = viewModel
            lifecycleOwner = this@LocationLineHolder.lifecycleOwner
            binding.executePendingBindings()
        }
    }
}

object DayLineDiff : DiffUtil.ItemCallback<ScheduleDay>() {
    override fun areItemsTheSame(oldItem: ScheduleDay, newItem: ScheduleDay) = oldItem.day == newItem.day
    override fun areContentsTheSame(oldItem: ScheduleDay, newItem: ScheduleDay) = oldItem.location == newItem.location
}

object ClassLineDiff : DiffUtil.ItemCallback<LocationWithGroup>() {
    override fun areItemsTheSame(oldItem: LocationWithGroup, newItem: LocationWithGroup) = oldItem.location.uid == newItem.location.uid
    override fun areContentsTheSame(oldItem: LocationWithGroup, newItem: LocationWithGroup) = oldItem.location == newItem.location
}

data class ScheduleDay(
    val day: String,
    val location: List<LocationWithGroup>
)

// -------------------------------------- Schedule Block --------------------------------------------

class ScheduleBlockAdapter(
    private val pool: RecyclerView.RecycledViewPool,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: ScheduleViewModel,
    context: Context,
    private val showHidden: Boolean
) : RecyclerView.Adapter<DayBlockHolder>() {
    private val list = ArrayList<ArrayList<InnerLocation>>()
    private val colors = context.resources.getIntArray(R.array.discipline_colors)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayBlockHolder {
        val binding = ItemScheduleDayBinding.inflate(parent.inflater(), parent, false)
        return DayBlockHolder(binding, pool, colors, lifecycleOwner, viewModel)
    }

    override fun onBindViewHolder(holder: DayBlockHolder, position: Int) = holder.bind(list[position])
    override fun getItemCount() = list.size
    override fun getItemViewType(position: Int) = DAY

    fun adaptList(location: List<LocationWithGroup>) {
        list.clear()

        val mapping = HashMap<String, ArrayList<InnerLocation>>()
        val times = ArrayList<ClassTime>()
        val colors = HashMap<String, Int>()
        var index = 0

        location.forEach { l ->
            val day = l.location.day
            val classes = mapping.getOrPut(day) { ArrayList() }
            val time = ClassTime(l.location.startsAt, l.location.endsAt)

            val code = l.singleGroup().singleClass().singleDiscipline().code
            if (!colors.containsKey(code)) colors[code] = index++
            val color = colors[code]

            // working by reference... lul
            classes.add(InnerLocation(location = l, time = time, colorIndex = color))
            if (!times.contains(time)) times.add(time)
        }

        if (times.isEmpty()) return
        times.sort()

        val line = ArrayList<InnerLocation>()

        line.add(InnerLocation(nothing = true))
        times.forEach {
            line.add(InnerLocation(time = it, timeRow = true))
        }
        list.add(line)

        for (i in 1..7) {
            val day = i.toWeekDay()
            val classes = mapping[day]
            if (classes != null && classes.isNotEmpty()) {
                classes.sort()

                val full = ArrayList<InnerLocation>()
                full.add(InnerLocation(day = day, header = true))
                times.forEach {
                    val position = classes.positionOf(it)
                    if (position == -1) full.add(InnerLocation())
                    else full.add(classes[position])
                }
                list.add(full)
            } else if (showHidden && i > 1) {
                val full = ArrayList<InnerLocation>()
                full.add(InnerLocation(day = day, header = true))
                times.forEach { _ -> full.add(InnerLocation()) }
                list.add(full)
            }
        }
        notifyDataSetChanged()
    }
}

// ==================================================================================================
private const val HEADER: Int = 0
private const val TIME: Int = 1
private const val CLASS: Int = 2
private const val NOTHING: Int = 3
private const val HEAD_N: Int = 4
private const val DAY: Int = 5

class ScheduleBlockClassAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: ScheduleViewModel,
    private val colors: IntArray
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val list = ArrayList<InnerLocation>()

    fun submitList(list: List<InnerLocation>) {
        this.list.clear()
        this.list.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER -> BHeaderHolder(ItemScheduleBlockHeaderBinding.inflate(parent.inflater(), parent, false))
            TIME -> BTimeHolder(ItemScheduleBlockTimeBinding.inflate(parent.inflater(), parent, false))
            CLASS -> BClassHolder(ItemScheduleBlockClassBinding.inflate(parent.inflater(), parent, false))
            HEAD_N -> BHeadNotHolder(ItemScheduleBlockHeadNotBinding.inflate(parent.inflater(), parent, false))
            else -> BNothingHolder(ItemScheduleBlockNothingBinding.inflate(parent.inflater(), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val element = list[position]
        when (getItemViewType(position)) {
            HEADER -> (holder as BHeaderHolder).bind(element)
            TIME -> (holder as BTimeHolder).bind(element)
            CLASS -> (holder as BClassHolder).bind(element, colors).also {
                holder.binding.lifecycleOwner = lifecycleOwner
                holder.binding.scheduleActions = viewModel
                holder.binding.group = element.location!!.singleGroup()
                holder.binding.executePendingBindings()
            }
        }
    }

    override fun getItemCount() = list.size

    override fun getItemViewType(position: Int): Int {
        val item = list[position]
        return when {
            item.header -> return HEADER
            item.timeRow -> return TIME
            item.location != null -> CLASS
            item.nothing -> HEAD_N
            else -> NOTHING
        }
    }
}

class DayBlockHolder(
    binding: ItemScheduleDayBinding,
    pool: RecyclerView.RecycledViewPool,
    colors: IntArray,
    lifecycleOwner: LifecycleOwner,
    scheduleViewModel: ScheduleViewModel
) : RecyclerView.ViewHolder(binding.root) {
    private val adapter by lazy { ScheduleBlockClassAdapter(lifecycleOwner, scheduleViewModel, colors) }
    init {
        binding.recyclerDay.setRecycledViewPool(pool)
        binding.recyclerDay.adapter = adapter
    }

    fun bind(list: ArrayList<InnerLocation>) {
        adapter.submitList(list)
    }
}

class BHeaderHolder(
    private val binding: ItemScheduleBlockHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(inner: InnerLocation) {
        binding.textHeader.text = inner.day
    }
}

class BTimeHolder(
    private val binding: ItemScheduleBlockTimeBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(inner: InnerLocation) {
        binding.inner = inner
        binding.executePendingBindings()
    }
}

class BClassHolder(
    val binding: ItemScheduleBlockClassBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(inner: InnerLocation, colors: IntArray) {
        binding.tvCode.text = inner.location!!.singleGroup().singleClass().singleDiscipline().code
        binding.tvGroup.text = inner.location.singleGroup().group.group
        val color = colors[(inner.colorIndex ?: 0) % colors.size]
        binding.cardRoot.strokeColor = color
        binding.cardRoot.setCardBackgroundColor(ColorUtils.modifyAlpha(color, 40))
    }
}

class BNothingHolder(
    binding: ItemScheduleBlockNothingBinding
) : RecyclerView.ViewHolder(binding.root)

class BHeadNotHolder(
    binding: ItemScheduleBlockHeadNotBinding
) : RecyclerView.ViewHolder(binding.root)

class ClassTime(
    val start: String,
    val end: String
) : Comparable<ClassTime> {

    override fun compareTo(other: ClassTime): Int {
        return start.compareTo(other.start)
    }

    override fun equals(other: Any?): Boolean {
        if (other is ClassTime) {
            return other.start == start && other.end == end
        } else if (other is InnerLocation) {
            return other.time == this
        }
        return false
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }
}

class InnerLocation(
    val location: LocationWithGroup? = null,
    val time: ClassTime? = null,
    val colorIndex: Int? = 0,
    val day: String? = null,
    val header: Boolean = false,
    val timeRow: Boolean = false,
    val nothing: Boolean = false
) : Comparable<InnerLocation> {

    override fun compareTo(other: InnerLocation): Int {
        if (header) return -1
        return time?.compareTo(other.time!!) ?: 0
    }

    override fun equals(other: Any?): Boolean {
        if (other is InnerLocation) {
            return other.location?.location?.uid == location?.location?.uid
        } else if (other is ClassTime) {
            return other == time
        }
        return false
    }

    override fun hashCode(): Int {
        var result = location?.hashCode() ?: 0
        result = 31 * result + (time?.hashCode() ?: 0)
        result = 31 * result + (colorIndex ?: 0)
        result = 31 * result + header.hashCode()
        result = 31 * result + timeRow.hashCode()
        result = 31 * result + nothing.hashCode()
        return result
    }
}
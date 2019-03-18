package com.forcetower.sagres.operation.disciplines

import com.forcetower.sagres.database.model.SDiscipline
import com.forcetower.sagres.database.model.SDisciplineGroup
import com.forcetower.sagres.operation.BaseCallback
import com.forcetower.sagres.operation.Status

class FastDisciplinesCallback(status: Status) : BaseCallback<FastDisciplinesCallback>(status) {
    private var groups: List<SDisciplineGroup> = emptyList()
    private var flags: Int = 0
    private var current: Int = 0
    private var total: Int = 0
    private var failureCount: Int = 0
    private var semesters: List<Pair<Long, String>> = emptyList()

    fun getFlags() = flags
    fun getGroups() = groups
    fun getCurrent() = current
    fun getTotal() = total
    fun getFailureCount() = failureCount
    fun getSemesters() = semesters

    fun getDisciplines(): List<SDiscipline> {
        return groups.groupBy { it.code }.map { entry ->
            val value = entry.value
            val code = entry.key

            val creditsSum = value.groupBy { it.semester }.map { it.value.sumBy { group -> group.credits } }.max() ?: 0
            val first = value.first()
            SDiscipline(first.semester, first.name, code).apply {
                credits = creditsSum
            }
        }
    }

    fun groups(groups: List<SDisciplineGroup>): FastDisciplinesCallback {
        this.groups = groups
        return this
    }

    fun flags(flags: Int = 0): FastDisciplinesCallback {
        this.flags = flags
        return this
    }

    fun current(current: Int): FastDisciplinesCallback {
        this.current = current
        return this
    }

    fun total(total: Int): FastDisciplinesCallback {
        this.total = total
        return this
    }

    fun failureCount(failureCount: Int): FastDisciplinesCallback {
        this.failureCount = failureCount
        return this
    }

    fun semesters(semesters: List<Pair<Long, String>>): FastDisciplinesCallback {
        this.semesters = semesters
        return this
    }

    companion object {
        fun copyFrom(callback: BaseCallback<*>): FastDisciplinesCallback {
            return FastDisciplinesCallback(callback.status).message(callback.message).code(callback.code).throwable(
                    callback.throwable).document(callback.document)
        }

        const val LOGIN = 1
        const val INITIAL = 2
        const val PROCESSING = 4
        const val DOWNLOADING = 8
        const val SAVING = 16
        const val GRADES = 32
    }
}
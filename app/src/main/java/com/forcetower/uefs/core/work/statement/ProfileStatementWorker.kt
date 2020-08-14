package com.forcetower.uefs.core.work.statement

import android.content.Context
import androidx.annotation.IntRange
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.forcetower.uefs.core.model.unes.ProfileStatement
import com.forcetower.uefs.core.storage.repository.ProfileRepository
import com.forcetower.uefs.core.work.enqueue
import timber.log.Timber
import javax.inject.Inject

class ProfileStatementWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : Worker(context, params) {
    @Inject
    lateinit var repository: ProfileRepository

    override fun doWork(): Result {
        val statementId = inputData.getLong(STATEMENT, 0)
        return try {
            when (inputData.getInt(OPERATION, 0)) {
                ACCEPT -> processResult(repository.acceptStatement(statementId))
                REFUSE -> processResult(repository.refuseStatement(statementId))
                DELETE -> processResult(repository.deleteStatement(statementId))
                else -> Result.success()
            }
        } catch (error: Throwable) {
            Timber.e(error, "A error occurred when sending statement signal")
            Result.retry()
        }
    }

    private fun processResult(result: Int): Result {
        return when (result) {
            0 -> Result.success()
            1 -> Result.retry()
            else -> Result.failure()
        }
    }

    companion object {
        private const val TAG = "profile_statement_operation"
        const val ACCEPT = 1
        const val REFUSE = 2
        const val DELETE = 3
        private const val OPERATION = "statement_operation"
        private const val STATEMENT = "statement"

        fun createWorker(
            context: Context,
            statement: ProfileStatement,
            @IntRange(from = 1, to = 3) operation: Int
        ) {
            val data = workDataOf(
                OPERATION to operation,
                STATEMENT to statement.id
            )
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

            OneTimeWorkRequestBuilder<ProfileStatementWorker>()
                .setInputData(data)
                .addTag(TAG)
                .setConstraints(constraints)
                .build()
                .enqueue(context)
        }
    }
}
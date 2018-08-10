package com.forcetower.unes.core.model.sagres

import androidx.room.Entity
import com.forcetower.unes.core.model.Identifiable

@Entity
data class SagresAccess(
       val username: String,
       val password: String
): Identifiable()
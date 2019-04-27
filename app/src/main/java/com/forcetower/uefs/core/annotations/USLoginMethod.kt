package com.forcetower.uefs.core.annotations

import androidx.annotation.StringDef
import com.forcetower.uefs.core.storage.repository.cloud.AuthRepository

@Retention(AnnotationRetention.SOURCE)
@StringDef(value = [AuthRepository.LOGIN_METHOD_UNES, AuthRepository.LOGIN_METHOD_SAGRES])
annotation class USLoginMethod
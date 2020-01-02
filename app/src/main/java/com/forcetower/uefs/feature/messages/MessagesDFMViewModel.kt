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

package com.forcetower.uefs.feature.messages

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.vm.Event
import com.forcetower.uefs.feature.shared.UFragment
import com.google.android.play.core.splitinstall.SplitInstallException
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import timber.log.Timber
import javax.inject.Inject

class MessagesDFMViewModel @Inject constructor(
    context: Context
) : ViewModel() {
    companion object {
        const val AERI_MODULE = "aeri"
    }

    private var enabled = false

    private val splitInstallManager = SplitInstallManagerFactory.create(context)
    private var sessionId = 0

    private val _snackbar = MutableLiveData<Event<String>>()
    val snackbarMessage: LiveData<Event<String>>
        get() = _snackbar

    private val _downloadStatus = MutableLiveData<Pair<Long, Long>>()
    val downloadStatus: LiveData<Pair<Long, Long>>
        get() = _downloadStatus

    private val _sessionStatus = MutableLiveData<Event<Int>>()
    val sessionStatus: LiveData<Event<Int>>
        get() = _sessionStatus

    private val listener = SplitInstallStateUpdatedListener { state ->
        if (state.sessionId() == sessionId) {
            _sessionStatus.value = Event(state.status())
            when (state.status()) {
                SplitInstallSessionStatus.FAILED -> {
                    Timber.d("Module install failed with ${state.errorCode()}")
                    showProperSplitInstallError(state.errorCode())
                }
                SplitInstallSessionStatus.INSTALLED -> {
                    _snackbar.value = Event("O módulo novo foi instalado!!!")
                }
                SplitInstallSessionStatus.DOWNLOADING -> {
                    val total = state.totalBytesToDownload()
                    val downloaded = state.bytesDownloaded()
                    _downloadStatus.value = downloaded to total
                }
                else -> {
                    Timber.d("Status: ${state.status()}")
                }
            }
        }
    }

    init {
        splitInstallManager.registerListener(listener)
    }

    fun isAeriInstalled() = splitInstallManager.installedModules.contains(AERI_MODULE)

    fun requestAERIInstall() {
        val request = SplitInstallRequest
            .newBuilder()
            .addModule(AERI_MODULE)
            .build()

        splitInstallManager
            .startInstall(request)
            .addOnSuccessListener { id -> sessionId = id }
            .addOnFailureListener { exception ->
                when (exception) {
                    is SplitInstallException -> {
                        showProperSplitInstallError(exception.errorCode)
                    }
                }
                Timber.e("Error installing module: $exception", exception)
            }
    }

    private fun showProperSplitInstallError(errorCode: Int) {
        val message = when (errorCode) {
            SplitInstallErrorCode.NETWORK_ERROR -> "Internet ruim"
            SplitInstallErrorCode.MODULE_UNAVAILABLE -> "O modulo não existe ainda..."
            else -> "Um problema qualquer, com código $errorCode"
        }
        _snackbar.value = Event(message)
    }

    override fun onCleared() {
        splitInstallManager.unregisterListener(listener)
        super.onCleared()
    }

    fun aeriReflectInstance(): UFragment {
        val instance = Class.forName("com.forcetower.uefs.aeri.feature.AERINewsFragment")
        return instance.newInstance() as UFragment
    }
}
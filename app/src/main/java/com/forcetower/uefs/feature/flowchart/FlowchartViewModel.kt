package com.forcetower.uefs.feature.flowchart

import androidx.lifecycle.ViewModel
import com.forcetower.uefs.core.storage.repository.FlowchartRepository
import javax.inject.Inject

class FlowchartViewModel @Inject constructor(
    private val repository: FlowchartRepository
) : ViewModel()
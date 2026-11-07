package dev.forcetower.unes.ui.feature.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialDisciplineRef
import dev.forcetower.melon.feature.materials.domain.model.MaterialFileKind
import dev.forcetower.melon.feature.materials.domain.model.MaterialStatus
import dev.forcetower.melon.feature.materials.domain.model.MaterialType
import dev.forcetower.melon.feature.materials.domain.model.MaterialUploader
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDiscipline
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

internal object MaterialsTestFixtures {
    val calculus = discipline(id = "d1", code = "SYN101")
    val physics = discipline(id = "d2", code = "SYN202")

    fun discipline(
        id: String,
        code: String,
    ) = MaterialsDiscipline(
        id = id,
        code = code,
        name = "Disciplina sintética $code",
        teacherName = null,
        counts = emptyMap(),
    )

    fun material(
        id: String,
        discipline: MaterialsDiscipline = calculus,
        usefulCount: Int = 0,
        downloadCount: Int = 0,
        isUseful: Boolean = false,
        isSaved: Boolean = false,
    ) = Material(
        id = id,
        discipline = MaterialDisciplineRef(id = discipline.id, code = discipline.code, name = discipline.name),
        type = MaterialType.Exam,
        title = "Prova sintética $id",
        teacherName = null,
        semester = "2026.1",
        pages = 2,
        fileKind = MaterialFileKind.Pdf,
        usefulCount = usefulCount,
        downloadCount = downloadCount,
        uploader = MaterialUploader(course = "Curso sintético", entryYear = 2024),
        note = null,
        isMine = false,
        status = MaterialStatus.Published,
        rejectionReason = null,
        isUseful = isUseful,
        isSaved = isSaved,
    )
}

// The view models read and write files on Dispatchers.IO, which the test
// scheduler does not drive: keep advancing until everything they launched is done.
@OptIn(ExperimentalCoroutinesApi::class)
internal fun TestScope.settle(model: ViewModel) {
    val work = model.viewModelScope.coroutineContext.job
    repeat(1_000) {
        advanceUntilIdle()
        if (work.children.all { it.isCompleted }) return
        Thread.sleep(5)
    }
    error("view model work did not settle")
}

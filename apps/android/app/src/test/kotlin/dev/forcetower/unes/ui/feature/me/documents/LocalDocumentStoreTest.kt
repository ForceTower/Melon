package dev.forcetower.unes.ui.feature.me.documents

import android.content.Context
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.core.session.domain.model.AuthState
import dev.forcetower.melon.core.session.domain.model.User
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

internal class LocalDocumentStoreTest {
    private val filesDir = createTempDirectory("local-documents").toFile()
    private val context = mockk<Context>().also { every { it.filesDir } returns filesDir }
    private val sessionStore = mockk<SessionStore>()
    private val store = LocalDocumentStore(context, sessionStore)

    @AfterTest
    fun tearDown() {
        filesDir.deleteRecursively()
    }

    @Test
    fun aSavedCopyOnlyOpensForTheAccountThatSavedIt() =
        runTest {
            signIn("ana")
            store.save(AcademicDocument.AcademicHistory, SYNTHETIC_PDF)
            assertNotNull(store.load(AcademicDocument.AcademicHistory))

            signIn("bruno")
            assertNull(store.load(AcademicDocument.AcademicHistory))
        }

    @Test
    fun aCopySavedBeforeOwnersWereRecordedNeverOpens() =
        runTest {
            val directory = File(filesDir, "documents").apply { mkdirs() }
            File(directory, "historico-escolar.pdf").writeBytes(SYNTHETIC_PDF)
            File(directory, "historico-escolar.pdf.meta.json").writeText("""{"version":1,"savedAtMs":0}""")
            signIn("bruno")

            assertNull(store.load(AcademicDocument.AcademicHistory))
        }

    @Test
    fun clearDeletesEveryCopy() =
        runTest {
            signIn("ana")
            store.save(AcademicDocument.EnrollmentCertificate, SYNTHETIC_PDF)

            store.clear()

            assertNull(store.load(AcademicDocument.EnrollmentCertificate))
            assertFalse(File(filesDir, "documents").exists())
        }

    private fun signIn(userId: String) {
        coEvery { sessionStore.currentAuthState() } returns AuthState.Authenticated(User(userId, "Estudante", null))
    }

    private companion object {
        val SYNTHETIC_PDF = "%PDF-synthetic".encodeToByteArray()
    }
}

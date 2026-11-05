package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ProfileEntity
import com.example.domain.qr.QrCodeDraft
import com.example.domain.qr.QrCodeTemplate
import com.example.domain.qr.QrCodeTemplateCatalog
import com.example.domain.qr.QrCodeType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QrCodeRepositoryUnitTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: QrCodeRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.profileDao().insertProfile(ProfileEntity.createDefaultPersonal())
        database.profileDao().insertProfile(ProfileEntity(id = 2L, name = "Work", type = "WORK"))
        repository = QrCodeRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bundleImportsAtomicallyAndRepeatImportDoesNotDuplicate() = runBlocking {
        repository.save(
            profileId = 1L,
            draft = QrCodeDraft("My own code", QrCodeType.TEXT, listOf("keep this record"))
        )

        val first = repository.importTemplates(1L, QrCodeTemplateCatalog.testingBundle)
        val repeated = repository.importTemplates(1L, QrCodeTemplateCatalog.testingBundle)

        assertEquals(8, first.imported)
        assertEquals(0, first.alreadyPresent)
        assertEquals(0, repeated.imported)
        assertEquals(8, repeated.alreadyPresent)
        assertEquals(9, database.qrCodeDao().getAll().count { it.profileId == 1L })
    }

    @Test
    fun invalidBundleWritesNoRecords() = runBlocking {
        val invalid = QrCodeTemplate(
            id = "qr-invalid",
            title = "Invalid website",
            description = "Bad test payload",
            type = QrCodeType.URL,
            fields = listOf("file:///private")
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.importTemplates(
                    1L,
                    listOf(QrCodeTemplateCatalog.testingBundle.first(), invalid)
                )
            }
        }
        assertEquals(0, database.qrCodeDao().getAll().size)
    }

    @Test
    fun templateIdentityIsIsolatedPerProfile() = runBlocking {
        val template = QrCodeTemplateCatalog.testingBundle.first()

        repository.importTemplates(1L, listOf(template))
        repository.importTemplates(2L, listOf(template))

        assertEquals(2, database.qrCodeDao().getAll().count { it.sourceTemplateId == template.id })
        assertEquals(1, database.qrCodeDao().getAll().count { it.profileId == 1L })
        assertEquals(1, database.qrCodeDao().getAll().count { it.profileId == 2L })
    }

    @Test
    fun editUpdatesTheExistingRecordWithoutChangingItsIdentity() = runBlocking {
        val original = repository.save(1L, QrCodeDraft("First", QrCodeType.TEXT, listOf("before")))
        val edited = repository.save(1L, QrCodeDraft("Updated", QrCodeType.URL, listOf("https://example.com")), original.id)

        assertEquals(original.id, edited.id)
        assertEquals("Updated", edited.title)
        assertEquals("https://example.com", edited.payload)
        assertEquals(1, database.qrCodeDao().getAll().count { it.profileId == 1L })
    }
}

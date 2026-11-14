package com.br.androideventscheduler.feature.backup.data.repository

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.database.RememberMeDatabase
import com.br.androideventscheduler.core.schedule.data.repository.ScheduleRepositoryImpl
import com.br.androideventscheduler.core.schedule.domain.recurrence.RecurrenceEngine
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.feature.backup.data.codec.AgendaJsonCodec
import com.br.androideventscheduler.feature.backup.data.file.DocumentStreams
import com.br.androideventscheduler.feature.backup.data.file.ExportFileStore
import com.br.androideventscheduler.feature.backup.data.fileJson
import com.br.androideventscheduler.feature.backup.data.fullyPopulatedItems
import com.br.androideventscheduler.feature.backup.data.itemJson
import com.br.androideventscheduler.feature.backup.domain.model.BackupError
import com.br.androideventscheduler.feature.backup.domain.model.ExportTarget
import com.br.androideventscheduler.feature.backup.testing.resetFileProviderCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AgendaBackupRepositoryImplTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: RememberMeDatabase
    private lateinit var scheduleRepository: ScheduleRepositoryImpl
    private lateinit var repository: AgendaBackupRepositoryImpl
    private val codec = AgendaJsonCodec()
    private val clock = FakeClock(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC)

    private fun contentUri(name: String): Uri = Uri.parse("content://com.example.mail.provider/attachments/$name")

    private fun exportsDir() = File(context.cacheDir, "exports")

    @Before
    fun setUp() {
        resetFileProviderCache()
        database =
            Room
                .inMemoryDatabaseBuilder(context, RememberMeDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val dispatcher = UnconfinedTestDispatcher()
        scheduleRepository = ScheduleRepositoryImpl(database.scheduleDao(), RecurrenceEngine(), dispatcher)
        repository =
            AgendaBackupRepositoryImpl(
                scheduleRepository = scheduleRepository,
                codec = codec,
                exportFileStore = ExportFileStore(context),
                documentStreams = DocumentStreams(context),
                clock = clock,
                appVersion = "1.0",
                ioDispatcher = dispatcher,
            )
    }

    @After
    fun tearDown() {
        database.close()
        exportsDir().deleteRecursively()
    }

    private suspend fun populate() = scheduleRepository.upsertAll(fullyPopulatedItems())

    private fun registerFile(
        uri: Uri,
        text: String,
    ) = shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream(text.toByteArray()))

    private suspend fun readFailure(uri: Uri): BackupError {
        try {
            repository.readForImport(uri.toString())
        } catch (error: BackupError) {
            return error
        }
        fail("expected a BackupError")
        error("unreachable")
    }

    @Test
    fun `given stored items when exporting for sharing then a valid json with all items is written to the exports cache`() =
        runTest {
            populate()

            val result = repository.export(ExportTarget.ShareCache)

            assertEquals("rememberme-agenda-2026-11-08.json", result.fileName)
            assertEquals(4, result.itemCount)
            val file = File(exportsDir(), result.fileName)
            assertTrue(file.exists())
            val decoded = codec.decode(file.readText())
            assertEquals(scheduleRepository.getAll(), decoded.items)
        }

    @Test
    fun `given stored items when exporting for sharing then the uri comes from the restricted file provider`() =
        runTest {
            populate()

            val uri = Uri.parse(repository.export(ExportTarget.ShareCache).uri)

            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.fileprovider", uri.authority)
            assertEquals(listOf("exports", "rememberme-agenda-2026-11-08.json"), uri.pathSegments.takeLast(2))
        }

    @Test
    fun `given a previous export when exporting again then older files are removed`() =
        runTest {
            populate()
            exportsDir().mkdirs()
            File(exportsDir(), "rememberme-agenda-2020-01-01.json").writeText("{}")
            File(exportsDir(), "lixo.tmp").writeText("x")

            repository.export(ExportTarget.ShareCache)

            assertEquals(listOf("rememberme-agenda-2026-11-08.json"), exportsDir().list()!!.toList())
        }

    @Test
    fun `given a document uri when exporting then the json is written there and the uri is returned`() =
        runTest {
            populate()
            val uri = contentUri("saved.json")
            val sink = ByteArrayOutputStream()
            shadowOf(context.contentResolver).registerOutputStream(uri, sink)

            val result = repository.export(ExportTarget.Document(uri.toString()))

            assertEquals(uri.toString(), result.uri)
            assertEquals(4, codec.decode(sink.toString(Charsets.UTF_8)).items.size)
        }

    @Test
    fun `given an empty agenda when exporting then it fails with empty agenda and writes nothing`() =
        runTest {
            try {
                repository.export(ExportTarget.ShareCache)
                fail("expected EmptyAgenda")
            } catch (error: BackupError) {
                assertEquals(BackupError.EmptyAgenda, error)
            }

            assertFalse(exportsDir().exists() && exportsDir().list()!!.isNotEmpty())
        }

    @Test
    fun `given exported content when reading it for import then the preview round trips with all items existing`() =
        runTest {
            populate()
            val result = repository.export(ExportTarget.ShareCache)
            val uri = contentUri("roundtrip.json")
            registerFile(uri, File(exportsDir(), result.fileName).readText())

            val preview = repository.readForImport(uri.toString())

            assertEquals(fullyPopulatedItems().sortedBy { it.id }, preview.validItems.sortedBy { it.id })
            assertEquals(0, preview.newCount)
            assertEquals(4, preview.existingCount)
        }

    @Test
    fun `given a file with new existing and invalid items when reading then the preview counts each kind`() =
        runTest {
            scheduleRepository.upsert(scheduleItem(id = "existing", title = "Atual"))
            val uri = contentUri("mixed.json")
            registerFile(
                uri,
                fileJson(
                    listOf(
                        itemJson(id = "existing", title = "\"Do arquivo\""),
                        itemJson(id = "new-1"),
                        itemJson(id = "new-2"),
                        itemJson(id = "bad", startDate = "\"2026-99-99\""),
                    ).joinToString(","),
                ),
            )

            val preview = repository.readForImport(uri.toString())

            assertEquals(2, preview.newCount)
            assertEquals(1, preview.existingCount)
            assertEquals(1, preview.invalidCount)
            assertEquals(4, preview.totalInFile)
            assertEquals(setOf("existing"), preview.existingIds)
        }

    @Test
    fun `given a file when reading for import then nothing is written to the database`() =
        runTest {
            val uri = contentUri("preview-only.json")
            registerFile(uri, fileJson(itemJson(id = "x")))

            repository.readForImport(uri.toString())

            assertTrue(scheduleRepository.getAll().isEmpty())
        }

    @Test
    fun `given a file above the limit and no declared size when reading then it fails with too large without reading it all`() =
        runTest {
            val uri = contentUri("huge.json")
            val source = EndlessInputStream(totalBytes = 6L * 1024 * 1024)
            shadowOf(context.contentResolver).registerInputStream(uri, source)

            val error = readFailure(uri)

            assertEquals(BackupError.TooLarge, error)
            assertTrue("leu ${source.bytesRead} bytes", source.bytesRead <= 5L * 1024 * 1024 + 64 * 1024)
        }

    @Test
    fun `given a provider that declares a size above the limit when reading then it is refused before opening the stream`() =
        runTest {
            val uri = Uri.parse("content://$DECLARED_SIZE_AUTHORITY/attachments/declared-huge.json")
            Robolectric
                .setupContentProvider(DeclaredSizeProvider::class.java, DECLARED_SIZE_AUTHORITY)
                .declaredSize = 6L * 1024 * 1024
            val source = EndlessInputStream(totalBytes = 10)
            shadowOf(context.contentResolver).registerInputStream(uri, source)

            val error = readFailure(uri)

            assertEquals(BackupError.TooLarge, error)
            assertEquals(0L, source.bytesRead)
        }

    @Test
    fun `given a file of exactly the limit when reading then the size is accepted and the content is rejected as not a backup`() =
        runTest {
            val uri = contentUri("limit.json")
            registerFile(uri, "x".repeat(5 * 1024 * 1024))

            val error = readFailure(uri)

            assertEquals(BackupError.NotRememberMeFile, error)
        }

    @Test
    fun `given a file with utf8 bom when reading then it is parsed normally`() =
        runTest {
            val uri = contentUri("bom.json")
            registerFile(uri, "\uFEFF" + fileJson(itemJson(id = "bom")))

            assertEquals(listOf("bom"), repository.readForImport(uri.toString()).validItems.map { it.id })
        }

    @Test
    fun `given files that are not valid backups when reading then the failure is typed and nothing is written`() =
        runTest {
            val other = contentUri("other.json")
            val newer = contentUri("newer.json")
            val broken = contentUri("broken.json")
            val empty = contentUri("empty.json")
            val allInvalid = contentUri("all-invalid.json")
            registerFile(other, fileJson(itemJson(), format = "\"outro\""))
            registerFile(newer, fileJson(itemJson(), version = "7"))
            registerFile(broken, "{\"format\": \"rememberme-agenda\", \"ver")
            registerFile(empty, fileJson(""))
            registerFile(allInvalid, fileJson(itemJson(startDate = "\"x\"")))

            assertEquals(BackupError.NotRememberMeFile, readFailure(other))
            assertEquals(BackupError.UnsupportedVersion(7), readFailure(newer))
            assertEquals(BackupError.Corrupted, readFailure(broken))
            assertEquals(BackupError.EmptyAgenda, readFailure(empty))
            assertEquals(BackupError.Corrupted, readFailure(allInvalid))
            assertTrue(scheduleRepository.getAll().isEmpty())
        }

    @Test
    fun `given uris that are not content or cannot be opened when reading then the failure is io`() =
        runTest {
            assertEquals(BackupError.Io, readFailure(Uri.parse("file:///data/data/com.br.androideventscheduler/databases/remember_me.db")))
            assertEquals(BackupError.Io, readFailure(Uri.parse("http://example.com/agenda.json")))
            assertEquals(BackupError.Io, readFailure(contentUri("not-registered.json")))
        }

    /** Fonte que produz bytes sem fim prático e conta quantos foram lidos. */
    private class EndlessInputStream(
        private val totalBytes: Long,
    ) : InputStream() {
        var bytesRead = 0L
            private set

        override fun read(): Int = if (bytesRead < totalBytes) (++bytesRead).let { 'x'.code } else -1

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (bytesRead >= totalBytes) return -1
            val count = minOf(length.toLong(), totalBytes - bytesRead).toInt()
            buffer.fill('x'.code.toByte(), offset, offset + count)
            bytesRead += count
            return count
        }
    }

    /**
     * Provedor de teste que só responde à consulta de `OpenableColumns.SIZE` com [declaredSize]. O stream continua
     * vindo do `registerInputStream`, que o `ShadowContentResolver` consulta antes do provedor.
     */
    class DeclaredSizeProvider : ContentProvider() {
        var declaredSize: Long = 0L

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = MatrixCursor(arrayOf(OpenableColumns.SIZE)).apply { addRow(arrayOf<Any>(declaredSize)) }

        override fun getType(uri: Uri): String? = null

        override fun insert(
            uri: Uri,
            values: ContentValues?,
        ): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    private companion object {
        const val DECLARED_SIZE_AUTHORITY = "com.example.declaredsize.provider"
    }
}

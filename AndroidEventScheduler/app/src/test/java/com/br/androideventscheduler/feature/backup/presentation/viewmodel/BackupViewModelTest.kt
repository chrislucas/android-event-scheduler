package com.br.androideventscheduler.feature.backup.presentation.viewmodel

import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.testing.FakeStringProvider
import com.br.androideventscheduler.core.testing.FakeClock
import com.br.androideventscheduler.core.testing.MainDispatcherRule
import com.br.androideventscheduler.feature.backup.domain.model.BackupError
import com.br.androideventscheduler.feature.backup.domain.model.ExportResult
import com.br.androideventscheduler.feature.backup.domain.model.ExportTarget
import com.br.androideventscheduler.feature.backup.domain.model.ImportResult
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy
import com.br.androideventscheduler.feature.backup.domain.usecase.ImportAgendaUseCase
import com.br.androideventscheduler.feature.backup.presentation.effect.BackupUiEffect
import com.br.androideventscheduler.feature.backup.presentation.event.BackupUiEvent
import com.br.androideventscheduler.feature.backup.presentation.state.BackupUiState
import com.br.androideventscheduler.feature.backup.testing.FakeAgendaBackupRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeAgendaBackupRepository()
    private val importAgenda = mockk<ImportAgendaUseCase>()
    private val clock = FakeClock(Instant.parse("2026-11-08T15:00:00Z"), ZoneOffset.UTC)

    private fun createViewModel(pendingImportUri: String? = null) =
        BackupViewModel(
            pendingImportUri = pendingImportUri,
            backupRepository = repository,
            importAgenda = importAgenda,
            strings = FakeStringProvider(),
            clock = clock,
            dispatcher = mainRule.dispatcher,
        )

    private class EffectRecorder(
        val effects: MutableList<BackupUiEffect>,
        val job: Job,
    )

    private fun TestScope.recordEffects(viewModel: BackupViewModel): EffectRecorder {
        val effects = mutableListOf<BackupUiEffect>()
        return EffectRecorder(effects, launch { viewModel.uiEffect.collect { effects += it } })
    }

    private fun BackupViewModel.content(): BackupUiState.Content = uiState.value as BackupUiState.Content

    @Test
    fun `given stored items when view model starts then it shows loading and then the item count`() =
        runTest(mainRule.dispatcher) {
            repository.itemCount = 5

            val viewModel = createViewModel()
            assertEquals(BackupUiState.Loading, viewModel.uiState.value)
            advanceUntilIdle()

            assertEquals(BackupUiState.Content(itemCount = 5), viewModel.uiState.value)
        }

    @Test
    fun `given the count fails when view model starts then it shows zero items and an error message`() =
        runTest(mainRule.dispatcher) {
            repository.countError = IOException("db")
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)

            advanceUntilIdle()

            assertEquals(0, viewModel.content().itemCount)
            assertEquals(listOf(BackupUiEffect.ShowMessage(R.string.backup_load_error)), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given empty agenda when asking to share or save then nothing is exported or launched`() =
        runTest(mainRule.dispatcher) {
            repository.itemCount = 0
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnShareExportClicked)
            viewModel.onEvent(BackupUiEvent.OnSaveExportClicked)
            advanceUntilIdle()

            assertTrue(repository.exportTargets.isEmpty())
            assertTrue(recorder.effects.isEmpty())
            recorder.job.cancel()
        }

    @Test
    fun `given stored items when sharing then it exports to the cache and emits the share effect with subject and body`() =
        runTest(mainRule.dispatcher) {
            repository.exportResult = Result.success(ExportResult("content://fp/exports/a.json", "a.json", 3))
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnShareExportClicked)
            advanceUntilIdle()

            assertEquals(listOf<ExportTarget>(ExportTarget.ShareCache), repository.exportTargets)
            assertEquals(
                listOf(
                    BackupUiEffect.ShareFile(
                        uri = "content://fp/exports/a.json",
                        subject = "string(${R.string.backup_share_subject})[08/11/2026]",
                        body = "plural(${R.plurals.backup_share_body},3)[3]",
                    ),
                ),
                recorder.effects,
            )
            assertFalse(viewModel.content().isExporting)
            recorder.job.cancel()
        }

    @Test
    fun `given the export fails when sharing then it shows an error message and allows trying again`() =
        runTest(mainRule.dispatcher) {
            repository.exportResult = Result.failure(BackupError.Io)
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnShareExportClicked)
            advanceUntilIdle()

            assertEquals(listOf(BackupUiEffect.ShowMessage(R.string.backup_export_error)), recorder.effects)
            assertFalse(viewModel.content().isExporting)
            recorder.job.cancel()
        }

    @Test
    fun `given an export in progress when sharing again then the repeated tap is ignored`() =
        runTest(mainRule.dispatcher) {
            repository.exportGate = CompletableDeferred()
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnShareExportClicked)
            viewModel.onEvent(BackupUiEvent.OnShareExportClicked)
            viewModel.onEvent(BackupUiEvent.OnPickImportClicked)
            advanceUntilIdle()

            assertTrue(viewModel.content().isExporting)
            assertEquals(1, repository.exportTargets.size)
            repository.exportGate!!.complete(Unit)
            advanceUntilIdle()
            assertFalse(viewModel.content().isExporting)
        }

    @Test
    fun `given stored items when asking to save then it launches the create document picker with the dated file name`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnSaveExportClicked)
            viewModel.onEvent(BackupUiEvent.OnSaveExportClicked)
            advanceUntilIdle()

            assertEquals(listOf(BackupUiEffect.LaunchCreateDocument("rememberme-agenda-2026-11-08.json")), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given a created document when saving then it exports there and shows the saved message`() =
        runTest(mainRule.dispatcher) {
            repository.exportResult = Result.success(ExportResult("content://docs/x.json", "x.json", 3))
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnSaveDocumentCreated("content://docs/x.json"))
            advanceUntilIdle()

            assertEquals(listOf<ExportTarget>(ExportTarget.Document("content://docs/x.json")), repository.exportTargets)
            assertEquals(listOf(BackupUiEffect.ShowQuantityMessage(R.plurals.backup_saved_message, 3)), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given the save picker is cancelled when saving then nothing is exported and the button works again`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()
            viewModel.onEvent(BackupUiEvent.OnSaveExportClicked)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnSaveDocumentCreated(null))
            viewModel.onEvent(BackupUiEvent.OnSaveExportClicked)
            advanceUntilIdle()

            assertTrue(repository.exportTargets.isEmpty())
            assertEquals(2, recorder.effects.count { it is BackupUiEffect.LaunchCreateDocument })
            recorder.job.cancel()
        }

    @Test
    fun `given the picker button when pressed then it launches the open document picker once`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnPickImportClicked)
            viewModel.onEvent(BackupUiEvent.OnPickImportClicked)
            advanceUntilIdle()

            assertEquals(listOf(BackupUiEffect.LaunchOpenDocument), recorder.effects)
            recorder.job.cancel()
        }

    @Test
    fun `given a chosen valid file when reading then the preview is shown with the default replace strategy`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnImportFileChosen("content://mail/agenda.json"))
            advanceUntilIdle()

            assertEquals(listOf("content://mail/agenda.json"), repository.readUris)
            val content = viewModel.content()
            assertEquals(FakeAgendaBackupRepository.samplePreview(), content.preview)
            assertEquals(ImportStrategy.REPLACE_EXISTING, content.strategy)
            assertNull(content.importErrorRes)
            assertFalse(content.isReadingImport)
        }

    @Test
    fun `given the picker is cancelled when choosing a file then nothing is read`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnImportFileChosen(null))
            advanceUntilIdle()

            assertTrue(repository.readUris.isEmpty())
            assertNull(viewModel.content().preview)
        }

    @Test
    fun `given invalid files when reading then each reason is shown without a preview and nothing is imported`() =
        runTest(mainRule.dispatcher) {
            val expected =
                listOf(
                    BackupError.NotRememberMeFile to R.string.backup_error_not_backup,
                    BackupError.UnsupportedVersion(2) to R.string.backup_error_newer_version,
                    BackupError.Corrupted to R.string.backup_error_corrupted,
                    BackupError.TooLarge to R.string.backup_error_too_large,
                    BackupError.EmptyAgenda to R.string.backup_error_empty_file,
                    BackupError.Io to R.string.backup_error_io,
                )
            val viewModel = createViewModel()
            advanceUntilIdle()

            expected.forEach { (error, messageRes) ->
                repository.readResult = Result.failure(error)
                viewModel.onEvent(BackupUiEvent.OnImportFileChosen("content://x/file.json"))
                advanceUntilIdle()

                assertEquals(messageRes, viewModel.content().importErrorRes)
                assertNull(viewModel.content().preview)
            }
            coVerify(exactly = 0) { importAgenda(any(), any(), any()) }
        }

    @Test
    fun `given an unexpected failure when reading then it is shown as a generic read error`() =
        runTest(mainRule.dispatcher) {
            repository.readResult = Result.failure(IllegalStateException("boom"))
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnImportFileChosen("content://x/file.json"))
            advanceUntilIdle()

            assertEquals(R.string.backup_error_io, viewModel.content().importErrorRes)
        }

    @Test
    fun `given a pending import uri when view model starts then it opens directly on the preview`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel(pendingImportUri = "content://mail/attachment.json")

            advanceUntilIdle()

            assertEquals(listOf("content://mail/attachment.json"), repository.readUris)
            assertEquals(FakeAgendaBackupRepository.samplePreview(), viewModel.content().preview)
        }

    @Test
    fun `given a file being read when choosing another then the repeated request is ignored`() =
        runTest(mainRule.dispatcher) {
            repository.readGate = CompletableDeferred()
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnImportFileChosen("content://x/1.json"))
            viewModel.onEvent(BackupUiEvent.OnImportFileChosen("content://x/2.json"))
            advanceUntilIdle()

            assertTrue(viewModel.content().isReadingImport)
            assertEquals(listOf("content://x/1.json"), repository.readUris)
            repository.readGate!!.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `given a preview when selecting a strategy then it is kept and used on confirm`() =
        runTest(mainRule.dispatcher) {
            coEvery { importAgenda(any(), any(), any()) } returns Result.success(ImportResult(3, 0, 0))
            val viewModel = createViewModel(pendingImportUri = "content://x/f.json")
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnStrategySelected(ImportStrategy.IMPORT_AS_COPIES))
            viewModel.onEvent(BackupUiEvent.OnConfirmImportClicked)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                importAgenda(
                    FakeAgendaBackupRepository.samplePreview(),
                    ImportStrategy.IMPORT_AS_COPIES,
                    "string(${R.string.backup_copy_title_suffix})",
                )
            }
        }

    @Test
    fun `given a confirmed import when it succeeds then it shows the count and navigates back`() =
        runTest(mainRule.dispatcher) {
            coEvery { importAgenda(any(), any(), any()) } returns Result.success(ImportResult(3, 0, 0))
            val viewModel = createViewModel(pendingImportUri = "content://x/f.json")
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnConfirmImportClicked)
            advanceUntilIdle()

            assertEquals(
                listOf(BackupUiEffect.ShowQuantityMessage(R.plurals.backup_imported_message, 3), BackupUiEffect.NavigateBack),
                recorder.effects,
            )
            assertNull(viewModel.content().preview)
            recorder.job.cancel()
        }

    @Test
    fun `given alarms that could not be scheduled when importing then a warning is shown before going back`() =
        runTest(mainRule.dispatcher) {
            coEvery { importAgenda(any(), any(), any()) } returns Result.success(ImportResult(3, 0, 2))
            val viewModel = createViewModel(pendingImportUri = "content://x/f.json")
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnConfirmImportClicked)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    BackupUiEffect.ShowQuantityMessage(R.plurals.backup_imported_message, 3),
                    BackupUiEffect.ShowQuantityMessage(R.plurals.backup_alarm_failures_message, 2),
                    BackupUiEffect.NavigateBack,
                ),
                recorder.effects,
            )
            recorder.job.cancel()
        }

    @Test
    fun `given a confirmed import when it fails then the preview stays and an error is shown`() =
        runTest(mainRule.dispatcher) {
            coEvery { importAgenda(any(), any(), any()) } returns Result.failure(IOException("disk"))
            val viewModel = createViewModel(pendingImportUri = "content://x/f.json")
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnConfirmImportClicked)
            advanceUntilIdle()

            assertEquals(listOf(BackupUiEffect.ShowMessage(R.string.backup_import_error)), recorder.effects)
            assertEquals(FakeAgendaBackupRepository.samplePreview(), viewModel.content().preview)
            assertFalse(viewModel.content().isImporting)
            recorder.job.cancel()
        }

    @Test
    fun `given an import in progress when confirming again then the repeated tap is ignored`() =
        runTest(mainRule.dispatcher) {
            val gate = CompletableDeferred<Unit>()
            coEvery { importAgenda(any(), any(), any()) } coAnswers
                {
                    gate.await()
                    Result.success(ImportResult(3, 0, 0))
                }
            val viewModel = createViewModel(pendingImportUri = "content://x/f.json")
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnConfirmImportClicked)
            viewModel.onEvent(BackupUiEvent.OnConfirmImportClicked)
            viewModel.onEvent(BackupUiEvent.OnCancelImportClicked)
            viewModel.onEvent(BackupUiEvent.OnStrategySelected(ImportStrategy.KEEP_EXISTING))
            advanceUntilIdle()

            assertTrue(viewModel.content().isImporting)
            assertEquals(ImportStrategy.REPLACE_EXISTING, viewModel.content().strategy)
            assertTrue(viewModel.content().preview != null)
            gate.complete(Unit)
            advanceUntilIdle()
            coVerify(exactly = 1) { importAgenda(any(), any(), any()) }
        }

    @Test
    fun `given a preview when cancelling then it is discarded without importing`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel(pendingImportUri = "content://x/f.json")
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnCancelImportClicked)
            advanceUntilIdle()

            assertNull(viewModel.content().preview)
            coVerify(exactly = 0) { importAgenda(any(), any(), any()) }
        }

    @Test
    fun `given no preview when confirming then nothing is imported`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnConfirmImportClicked)
            advanceUntilIdle()

            coVerify(exactly = 0) { importAgenda(any(), any(), any()) }
        }

    @Test
    fun `given back clicked when handling the event then it navigates back`() =
        runTest(mainRule.dispatcher) {
            val viewModel = createViewModel()
            val recorder = recordEffects(viewModel)
            advanceUntilIdle()

            viewModel.onEvent(BackupUiEvent.OnBackClicked)
            advanceUntilIdle()

            assertEquals(listOf(BackupUiEffect.NavigateBack), recorder.effects)
            recorder.job.cancel()
        }
}

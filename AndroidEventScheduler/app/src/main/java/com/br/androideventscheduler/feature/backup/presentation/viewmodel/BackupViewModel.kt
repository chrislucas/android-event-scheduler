package com.br.androideventscheduler.feature.backup.presentation.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.common.StringProvider
import com.br.androideventscheduler.core.ui.format.AppLocale
import com.br.androideventscheduler.feature.backup.domain.model.BackupError
import com.br.androideventscheduler.feature.backup.domain.model.BackupFileName
import com.br.androideventscheduler.feature.backup.domain.model.ExportTarget
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy
import com.br.androideventscheduler.feature.backup.domain.repository.AgendaBackupRepository
import com.br.androideventscheduler.feature.backup.domain.usecase.ImportAgendaUseCase
import com.br.androideventscheduler.feature.backup.presentation.effect.BackupUiEffect
import com.br.androideventscheduler.feature.backup.presentation.event.BackupUiEvent
import com.br.androideventscheduler.feature.backup.presentation.state.BackupUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class BackupViewModel(
    private val pendingImportUri: String?,
    private val backupRepository: AgendaBackupRepository,
    private val importAgenda: ImportAgendaUseCase,
    private val strings: StringProvider,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Loading)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<BackupUiEffect>(Channel.BUFFERED)
    val uiEffect: Flow<BackupUiEffect> = _uiEffect.receiveAsFlow()

    /** `true` entre abrir um seletor do sistema e receber o resultado: ignora toques repetidos no botão. */
    private var isPickerOpen = false

    init {
        load()
    }

    fun onEvent(event: BackupUiEvent) {
        when (event) {
            BackupUiEvent.OnShareExportClicked -> shareExport()
            BackupUiEvent.OnSaveExportClicked -> requestSaveDestination()
            is BackupUiEvent.OnSaveDocumentCreated -> saveExport(event.uri)
            BackupUiEvent.OnPickImportClicked -> requestImportFile()
            is BackupUiEvent.OnImportFileChosen -> chooseImportFile(event.uri)
            is BackupUiEvent.OnStrategySelected -> selectStrategy(event.strategy)
            BackupUiEvent.OnConfirmImportClicked -> confirmImport()
            BackupUiEvent.OnCancelImportClicked -> cancelImport()
            BackupUiEvent.OnBackClicked -> sendEffect(BackupUiEffect.NavigateBack)
        }
    }

    private fun load() {
        viewModelScope.launch(dispatcher) {
            val count = runCatching { backupRepository.countItems() }
            _uiState.value = BackupUiState.Content(itemCount = count.getOrDefault(0))
            if (count.isFailure) _uiEffect.send(BackupUiEffect.ShowMessage(R.string.backup_load_error))
            pendingImportUri?.let { startReadImport(it) }
        }
    }

    /** Marca a operação como em andamento de forma síncrona e devolve `false` se a tela não aceita uma nova ação. */
    private inline fun tryStart(transform: (BackupUiState.Content) -> BackupUiState.Content): Boolean {
        val content = _uiState.value as? BackupUiState.Content
        if (content == null || content.isBusy) return false
        _uiState.value = transform(content)
        return true
    }

    private inline fun updateContent(transform: (BackupUiState.Content) -> BackupUiState.Content) {
        _uiState.update { state -> if (state is BackupUiState.Content) transform(state) else state }
    }

    private fun shareExport() {
        val content = _uiState.value as? BackupUiState.Content
        if (content?.canExport != true || isPickerOpen) return
        if (!tryStart { it.copy(isExporting = true) }) return
        viewModelScope.launch(dispatcher) {
            runCatching { backupRepository.export(ExportTarget.ShareCache) }
                .onSuccess { result ->
                    val subject = strings.getString(R.string.backup_share_subject, LocalDate.now(clock).format(subjectDateFormatter))
                    val body = strings.getQuantityString(R.plurals.backup_share_body, result.itemCount, result.itemCount)
                    _uiEffect.send(BackupUiEffect.ShareFile(uri = result.uri, subject = subject, body = body))
                }.onFailure { error -> _uiEffect.send(BackupUiEffect.ShowMessage(exportErrorRes(error))) }
            updateContent { it.copy(isExporting = false) }
        }
    }

    private fun requestSaveDestination() {
        val content = _uiState.value as? BackupUiState.Content
        if (content?.canExport != true || isPickerOpen) return
        isPickerOpen = true
        sendEffect(BackupUiEffect.LaunchCreateDocument(BackupFileName.forDate(LocalDate.now(clock))))
    }

    private fun saveExport(uri: String?) {
        isPickerOpen = false
        if (uri == null || !tryStart { it.copy(isExporting = true) }) return
        viewModelScope.launch(dispatcher) {
            runCatching { backupRepository.export(ExportTarget.Document(uri)) }
                .onSuccess { result ->
                    _uiEffect.send(BackupUiEffect.ShowQuantityMessage(R.plurals.backup_saved_message, result.itemCount))
                }.onFailure { error -> _uiEffect.send(BackupUiEffect.ShowMessage(exportErrorRes(error))) }
            updateContent { it.copy(isExporting = false) }
        }
    }

    private fun requestImportFile() {
        val content = _uiState.value as? BackupUiState.Content
        if (content == null || content.isBusy || isPickerOpen) return
        isPickerOpen = true
        sendEffect(BackupUiEffect.LaunchOpenDocument)
    }

    private fun chooseImportFile(uri: String?) {
        isPickerOpen = false
        if (uri == null) return
        startReadImport(uri)
    }

    /** Lê e valida o arquivo **sem gravar nada**; o resultado vira pré-visualização ou o motivo da recusa. */
    private fun startReadImport(uri: String) {
        if (!tryStart { it.copy(isReadingImport = true, preview = null, importErrorRes = null) }) return
        viewModelScope.launch(dispatcher) {
            runCatching { backupRepository.readForImport(uri) }
                .onSuccess { preview ->
                    updateContent {
                        it.copy(isReadingImport = false, preview = preview, strategy = ImportStrategy.REPLACE_EXISTING)
                    }
                }.onFailure { error ->
                    updateContent { it.copy(isReadingImport = false, importErrorRes = importErrorRes(error)) }
                }
        }
    }

    private fun selectStrategy(strategy: ImportStrategy) {
        updateContent { content -> if (content.preview != null && !content.isImporting) content.copy(strategy = strategy) else content }
    }

    private fun cancelImport() {
        updateContent { content ->
            if (content.isImporting) content else content.copy(preview = null, importErrorRes = null)
        }
    }

    private fun confirmImport() {
        val content = _uiState.value as? BackupUiState.Content ?: return
        val preview = content.preview ?: return
        val strategy = content.strategy
        if (!tryStart { it.copy(isImporting = true) }) return
        viewModelScope.launch(dispatcher) {
            val copySuffix = strings.getString(R.string.backup_copy_title_suffix)
            importAgenda(preview, strategy, copySuffix)
                .onSuccess { result ->
                    updateContent { it.copy(isImporting = false, preview = null) }
                    _uiEffect.send(BackupUiEffect.ShowQuantityMessage(R.plurals.backup_imported_message, result.importedCount))
                    if (result.alarmFailures > 0) {
                        _uiEffect.send(BackupUiEffect.ShowQuantityMessage(R.plurals.backup_alarm_failures_message, result.alarmFailures))
                    }
                    _uiEffect.send(BackupUiEffect.NavigateBack)
                }.onFailure {
                    updateContent { it.copy(isImporting = false) }
                    _uiEffect.send(BackupUiEffect.ShowMessage(R.string.backup_import_error))
                }
        }
    }

    /** Falha inesperada (não tipada) é tratada como erro de E/S. */
    private fun Throwable.asBackupError(): BackupError = this as? BackupError ?: BackupError.Io

    @StringRes
    private fun exportErrorRes(error: Throwable): Int =
        when (error.asBackupError()) {
            BackupError.EmptyAgenda -> R.string.backup_export_empty

            BackupError.NotRememberMeFile,
            is BackupError.UnsupportedVersion,
            BackupError.Corrupted,
            BackupError.TooLarge,
            BackupError.Io,
            -> R.string.backup_export_error
        }

    /** Erro de domínio → texto de resource; nunca a mensagem da exceção. */
    @StringRes
    private fun importErrorRes(error: Throwable): Int =
        when (error.asBackupError()) {
            BackupError.NotRememberMeFile -> R.string.backup_error_not_backup
            is BackupError.UnsupportedVersion -> R.string.backup_error_newer_version
            BackupError.Corrupted -> R.string.backup_error_corrupted
            BackupError.TooLarge -> R.string.backup_error_too_large
            BackupError.EmptyAgenda -> R.string.backup_error_empty_file
            BackupError.Io -> R.string.backup_error_io
        }

    private fun sendEffect(effect: BackupUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private companion object {
        val subjectDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", AppLocale)
    }
}

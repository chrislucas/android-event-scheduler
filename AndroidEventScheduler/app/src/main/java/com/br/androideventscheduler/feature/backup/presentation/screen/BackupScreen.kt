package com.br.androideventscheduler.feature.backup.presentation.screen

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.navigation.AgendaBackup
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy
import com.br.androideventscheduler.feature.backup.presentation.component.ExportSection
import com.br.androideventscheduler.feature.backup.presentation.component.ImportSection
import com.br.androideventscheduler.feature.backup.presentation.component.previewImport
import com.br.androideventscheduler.feature.backup.presentation.effect.BackupUiEffect
import com.br.androideventscheduler.feature.backup.presentation.event.BackupUiEvent
import com.br.androideventscheduler.feature.backup.presentation.state.BackupUiState
import com.br.androideventscheduler.feature.backup.presentation.viewmodel.BackupViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val SCREEN_HORIZONTAL_PADDING = 16.dp

/**
 * Ponto ÚNICO de disparo de UI externa ao app (share sheet e seletores de arquivo do sistema). A spec 16
 * (autenticação) chamará `suppressNextLock()` aqui, antes de [action], para que o app não bloqueie ao voltar.
 */
private inline fun launchExternalUi(action: () -> Unit) {
    action()
}

// Sem preview: injeta o ViewModel (Koin) e os launchers do sistema; as previews cobrem o conteúdo stateless BackupContent.
@Composable
internal fun BackupScreen(
    pendingImportUri: String?,
    onBack: () -> Unit,
    viewModel: BackupViewModel = koinViewModel { parametersOf(AgendaBackup(pendingImportUri)) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val shareNoAppMessage = stringResource(R.string.backup_share_no_app)
    val pickerUnavailableMessage = stringResource(R.string.backup_picker_unavailable)
    val createDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) { uri ->
            viewModel.onEvent(BackupUiEvent.OnSaveDocumentCreated(uri?.toString()))
        }
    val openDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            viewModel.onEvent(BackupUiEvent.OnImportFileChosen(uri?.toString()))
        }

    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            is BackupUiEffect.ShareFile -> {
                launchExternalUi {
                    if (!launchShareFileChooser(context, effect.uri, effect.subject, effect.body)) {
                        Toast.makeText(context, shareNoAppMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            is BackupUiEffect.LaunchCreateDocument -> {
                launchExternalUi {
                    try {
                        createDocumentLauncher.launch(effect.suggestedName)
                    } catch (_: ActivityNotFoundException) {
                        // Sem seletor no aparelho: libera o botão e avisa.
                        viewModel.onEvent(BackupUiEvent.OnSaveDocumentCreated(null))
                        Toast.makeText(context, pickerUnavailableMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            BackupUiEffect.LaunchOpenDocument -> {
                launchExternalUi {
                    try {
                        // "*/*" como alternativa: alguns provedores marcam .json como application/octet-stream.
                        openDocumentLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "*/*"))
                    } catch (_: ActivityNotFoundException) {
                        viewModel.onEvent(BackupUiEvent.OnImportFileChosen(null))
                        Toast.makeText(context, pickerUnavailableMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            is BackupUiEffect.ShowMessage -> {
                Toast.makeText(context, resources.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
            }

            is BackupUiEffect.ShowQuantityMessage -> {
                val text = resources.getQuantityString(effect.messageRes, effect.quantity, effect.quantity)
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }

            BackupUiEffect.NavigateBack -> {
                onBack()
            }
        }
    }

    BackupContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupContent(
    uiState: BackupUiState,
    onEvent: (BackupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(BackupUiEvent.OnBackClicked) },
                        modifier = Modifier.testTag(SemanticsTags.BACKUP_BACK_BUTTON),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            BackupUiState.Loading -> BackupLoading(Modifier.padding(innerPadding))
            is BackupUiState.Content -> BackupForm(content = uiState, onEvent = onEvent, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun BackupLoading(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.state_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .testTag(SemanticsTags.BACKUP_LOADING)
                    .semantics { contentDescription = loadingDescription },
        )
    }
}

@Composable
private fun BackupForm(
    content: BackupUiState.Content,
    onEvent: (BackupUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ExportSection(
            hasItems = content.itemCount > 0,
            enabled = content.canExport,
            isExporting = content.isExporting,
            onShareClick = { onEvent(BackupUiEvent.OnShareExportClicked) },
            onSaveClick = { onEvent(BackupUiEvent.OnSaveExportClicked) },
        )
        HorizontalDivider()
        ImportSection(
            content = content,
            onPickClick = { onEvent(BackupUiEvent.OnPickImportClicked) },
            onStrategySelect = { onEvent(BackupUiEvent.OnStrategySelected(it)) },
            onConfirm = { onEvent(BackupUiEvent.OnConfirmImportClicked) },
            onCancel = { onEvent(BackupUiEvent.OnCancelImportClicked) },
        )
    }
}

@Preview(name = "Com agendamentos", showBackground = true)
@Composable
private fun BackupContentPreview() {
    AndroidEventSchedulerTheme { BackupContent(uiState = BackupUiState.Content(itemCount = 12), onEvent = {}) }
}

@Preview(name = "Agenda vazia", showBackground = true)
@Composable
private fun BackupContentEmptyPreview() {
    AndroidEventSchedulerTheme { BackupContent(uiState = BackupUiState.Content(itemCount = 0), onEvent = {}) }
}

@Preview(name = "Exportando", showBackground = true)
@Composable
private fun BackupContentExportingPreview() {
    AndroidEventSchedulerTheme {
        BackupContent(uiState = BackupUiState.Content(itemCount = 12, isExporting = true), onEvent = {})
    }
}

@Preview(name = "Pré-visualização com repetidos e inválidos", showBackground = true)
@Composable
private fun BackupContentPreviewImportPreview() {
    AndroidEventSchedulerTheme {
        BackupContent(
            uiState = BackupUiState.Content(itemCount = 12, preview = previewImport(), strategy = ImportStrategy.KEEP_EXISTING),
            onEvent = {},
        )
    }
}

@Preview(name = "Importando", showBackground = true)
@Composable
private fun BackupContentImportingPreview() {
    AndroidEventSchedulerTheme {
        BackupContent(uiState = BackupUiState.Content(itemCount = 12, preview = previewImport(), isImporting = true), onEvent = {})
    }
}

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun BackupContentLoadingPreview() {
    AndroidEventSchedulerTheme { BackupContent(uiState = BackupUiState.Loading, onEvent = {}) }
}

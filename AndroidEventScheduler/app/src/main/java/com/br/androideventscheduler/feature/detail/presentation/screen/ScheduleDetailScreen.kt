package com.br.androideventscheduler.feature.detail.presentation.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.detail.presentation.component.DeleteConfirmationDialog
import com.br.androideventscheduler.feature.detail.presentation.component.DetailColorDot
import com.br.androideventscheduler.feature.detail.presentation.component.DetailInfoRow
import com.br.androideventscheduler.feature.detail.presentation.component.DetailRowIcon
import com.br.androideventscheduler.feature.detail.presentation.component.ScheduleAction
import com.br.androideventscheduler.feature.detail.presentation.component.ScheduleActionBar
import com.br.androideventscheduler.feature.detail.presentation.effect.ScheduleDetailUiEffect
import com.br.androideventscheduler.feature.detail.presentation.event.ScheduleDetailUiEvent
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUi
import com.br.androideventscheduler.feature.detail.presentation.state.ScheduleDetailUiState
import com.br.androideventscheduler.feature.detail.presentation.viewmodel.ScheduleDetailViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val SCREEN_HORIZONTAL_PADDING = 16.dp
private val COLOR_STRIPE_WIDTH = 8.dp

// Sem preview: injeta o ViewModel (Koin); as previews cobrem o conteúdo stateless ScheduleDetailContent.
@Composable
internal fun ScheduleDetailScreen(
    scheduleId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenCopy: (String) -> Unit,
    viewModel: ScheduleDetailViewModel = koinViewModel { parametersOf(scheduleId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            ScheduleDetailUiEffect.NavigateBack -> {
                onBack()
            }

            is ScheduleDetailUiEffect.NavigateToEdit -> {
                onEdit(effect.scheduleId)
            }

            is ScheduleDetailUiEffect.NavigateToCopy -> {
                onOpenCopy(effect.copyId)
            }

            // O Snackbar espera ser dispensado: roda em outra corrotina para não bloquear os próximos efeitos.
            is ScheduleDetailUiEffect.ShowCopyCreated -> {
                val message = context.getString(R.string.detail_copy_created)
                val actionLabel = context.getString(R.string.detail_copy_open_action)
                coroutineScope.launch {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = actionLabel,
                            duration = SnackbarDuration.Long,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onEvent(ScheduleDetailUiEvent.OnOpenCopyClicked(effect.copyId))
                    }
                }
            }

            // Ponto único de disparo do share sheet (a spec 16 chamará `suppressNextLock()` aqui, antes do Intent).
            is ScheduleDetailUiEffect.ShareText -> {
                if (!launchShareChooser(context, effect.subject, effect.body)) {
                    Toast.makeText(context, context.getString(R.string.share_error), Toast.LENGTH_SHORT).show()
                }
            }

            // Toast (e não Snackbar): a tela fecha logo em seguida e um Snackbar sumiria junto com ela.
            is ScheduleDetailUiEffect.ShowMessage -> {
                Toast.makeText(context, context.getString(effect.messageRes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    ScheduleDetailContent(uiState = uiState, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleDetailContent(
    uiState: ScheduleDetailUiState,
    onEvent: (ScheduleDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState, modifier = Modifier.testTag(SemanticsTags.DETAIL_SNACKBAR_HOST)) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_screen_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(ScheduleDetailUiEvent.OnBackClicked) },
                        modifier = Modifier.testTag(SemanticsTags.DETAIL_BACK_BUTTON),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (uiState is ScheduleDetailUiState.Content) {
                ScheduleActionBar(actions = detailActions(isEnabled = !uiState.isProcessingAction, onEvent = onEvent))
            }
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (uiState) {
            ScheduleDetailUiState.Loading -> DetailLoading(contentModifier)
            ScheduleDetailUiState.NotFound -> DetailNotFound(contentModifier)
            is ScheduleDetailUiState.Content -> DetailBody(schedule = uiState.schedule, modifier = contentModifier)
        }
    }

    if (uiState is ScheduleDetailUiState.Content && uiState.isDeleteDialogVisible) {
        DeleteConfirmationDialog(
            title = uiState.schedule.title,
            isRecurring = uiState.schedule.isRecurring,
            onConfirm = { onEvent(ScheduleDetailUiEvent.OnConfirmDeleteClicked) },
            onDismiss = { onEvent(ScheduleDetailUiEvent.OnDismissDeleteDialog) },
        )
    }
}

/** Duplicar (primeira), Editar, Compartilhar e Excluir (última, destrutiva). Todas desabilitadas enquanto uma ação roda. */
private fun detailActions(
    isEnabled: Boolean,
    onEvent: (ScheduleDetailUiEvent) -> Unit,
): List<ScheduleAction> =
    listOf(
        ScheduleAction(
            label = R.string.detail_duplicate_label,
            icon = R.drawable.ic_content_copy,
            tag = SemanticsTags.DETAIL_DUPLICATE_BUTTON,
            onClick = { onEvent(ScheduleDetailUiEvent.OnDuplicateClicked) },
            enabled = isEnabled,
            description = R.string.detail_duplicate_description,
        ),
        ScheduleAction(
            label = R.string.detail_edit_label,
            icon = R.drawable.ic_edit,
            tag = SemanticsTags.DETAIL_EDIT_BUTTON,
            onClick = { onEvent(ScheduleDetailUiEvent.OnEditClicked) },
            enabled = isEnabled,
            description = R.string.detail_edit_description,
        ),
        ScheduleAction(
            label = R.string.detail_share_label,
            icon = R.drawable.ic_share,
            tag = SemanticsTags.DETAIL_SHARE_BUTTON,
            onClick = { onEvent(ScheduleDetailUiEvent.OnShareClicked) },
            enabled = isEnabled,
            description = R.string.detail_share_description,
        ),
        ScheduleAction(
            label = R.string.detail_delete_label,
            icon = R.drawable.ic_delete,
            tag = SemanticsTags.DETAIL_DELETE_BUTTON,
            onClick = { onEvent(ScheduleDetailUiEvent.OnDeleteClicked) },
            enabled = isEnabled,
            description = R.string.detail_delete_description,
            isDestructive = true,
        ),
    )

@Composable
private fun DetailLoading(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.state_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .testTag(SemanticsTags.DETAIL_LOADING)
                    .semantics { contentDescription = loadingDescription },
        )
    }
}

@Composable
private fun DetailNotFound(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.detail_not_found),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(SemanticsTags.DETAIL_NOT_FOUND),
        )
    }
}

@Composable
private fun DetailBody(
    schedule: ScheduleDetailUi,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DetailTitleCard(schedule = schedule)
        DetailInfoRow(
            label = stringResource(R.string.detail_label_date),
            text = schedule.dateRangeText,
            tag = SemanticsTags.DETAIL_DATE_RANGE,
            leading = { DetailRowIcon(R.drawable.ic_schedule) },
        )
        DetailInfoRow(
            label = stringResource(R.string.detail_label_calendar),
            text = schedule.calendarLabel,
            tag = SemanticsTags.DETAIL_ACCOUNT_EMAIL,
            leading = { DetailColorDot(schedule.calendarColorHex) },
        )
        DetailInfoRow(
            label = stringResource(R.string.detail_label_reminder),
            text = schedule.reminderText,
            tag = SemanticsTags.DETAIL_REMINDER_LABEL,
            leading = { DetailRowIcon(R.drawable.ic_bell) },
        )
        DetailInfoRow(
            label = stringResource(R.string.detail_label_recurrence),
            text = schedule.recurrenceText,
            tag = SemanticsTags.DETAIL_RECURRENCE_LABEL,
            leading = { DetailRowIcon(R.drawable.ic_repeat) },
        )
        schedule.location?.let { location ->
            DetailInfoRow(
                label = stringResource(R.string.detail_label_location),
                text = location,
                tag = SemanticsTags.DETAIL_LOCATION,
                leading = { DetailRowIcon(R.drawable.ic_location) },
            )
        }
        schedule.notes?.let { notes ->
            DetailInfoRow(
                label = stringResource(R.string.detail_label_notes),
                text = notes,
                tag = SemanticsTags.DETAIL_NOTES,
                leading = { DetailRowIcon(R.drawable.ic_notes) },
            )
        }
    }
}

/** Cartão do título com faixa lateral na cor do item e adesivo (decorativo) antes do título. */
@Composable
private fun DetailTitleCard(schedule: ScheduleDetailUi) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier =
                    Modifier
                        .width(COLOR_STRIPE_WIDTH)
                        .fillMaxHeight()
                        .background(Color(schedule.colorHex))
                        .clearAndSetSemantics {},
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                schedule.stickerEmoji?.let { emoji ->
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier =
                        Modifier
                            .testTag(SemanticsTags.DETAIL_TITLE)
                            .semantics { heading() },
                )
            }
        }
    }
}

private val previewSchedule =
    ScheduleDetailUi(
        id = "preview",
        title = "Aula de Kotlin",
        stickerEmoji = "📚",
        colorHex = 0xFF5C6BC0,
        dateRangeText = "qui., 5 de nov., 22:00 → sex., 6 de nov., 23:30",
        calendarLabel = "christoffer.luccas@gmail.com",
        calendarColorHex = 0xFF29B6F6,
        reminderText = "10 minutos antes",
        recurrenceText = "Não repetir",
        location = "Sala 3, bloco B",
        notes = "Levar o notebook",
    )

@Preview(name = "Evento multi-dia", showBackground = true)
@Composable
private fun ScheduleDetailContentMultiDayPreview() {
    AndroidEventSchedulerTheme {
        ScheduleDetailContent(uiState = ScheduleDetailUiState.Content(previewSchedule), onEvent = {})
    }
}

@Preview(name = "Lembrete", showBackground = true)
@Composable
private fun ScheduleDetailContentReminderPreview() {
    AndroidEventSchedulerTheme {
        val reminder =
            previewSchedule.copy(
                title = "Pagar o aluguel",
                stickerEmoji = null,
                dateRangeText = "qui., 5 de nov., 22:00",
                calendarLabel = "Meus lembretes",
                calendarColorHex = 0xFFAB47BC,
                recurrenceText = "Diariamente",
            )
        ScheduleDetailContent(uiState = ScheduleDetailUiState.Content(reminder), onEvent = {})
    }
}

@Preview(name = "Dia inteiro", showBackground = true)
@Composable
private fun ScheduleDetailContentAllDayPreview() {
    AndroidEventSchedulerTheme {
        val allDay = previewSchedule.copy(dateRangeText = "qui., 5 de nov. · Todo o dia", reminderText = "Sem alerta")
        ScheduleDetailContent(uiState = ScheduleDetailUiState.Content(allDay), onEvent = {})
    }
}

@Preview(name = "Sem local nem notas", showBackground = true)
@Composable
private fun ScheduleDetailContentWithoutOptionalPreview() {
    AndroidEventSchedulerTheme {
        val minimal = previewSchedule.copy(location = null, notes = null, stickerEmoji = null)
        ScheduleDetailContent(uiState = ScheduleDetailUiState.Content(minimal), onEvent = {})
    }
}

@Preview(name = "Item longo", showBackground = true)
@Composable
private fun ScheduleDetailContentLongPreview() {
    AndroidEventSchedulerTheme {
        val long =
            previewSchedule.copy(
                title = "Workshop de arquitetura Android com Compose, Koin, Navigation 3 e testes automatizados de ponta a ponta",
                location = "Centro de convenções, pavilhão leste, sala 12, segundo andar, ao lado do auditório principal",
                notes = "Levar o notebook, o carregador, os documentos impressos e lembrar de confirmar a sala com a recepção.",
            )
        ScheduleDetailContent(uiState = ScheduleDetailUiState.Content(long), onEvent = {})
    }
}

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun ScheduleDetailContentLoadingPreview() {
    AndroidEventSchedulerTheme { ScheduleDetailContent(uiState = ScheduleDetailUiState.Loading, onEvent = {}) }
}

@Preview(name = "Não encontrado", showBackground = true)
@Composable
private fun ScheduleDetailContentNotFoundPreview() {
    AndroidEventSchedulerTheme { ScheduleDetailContent(uiState = ScheduleDetailUiState.NotFound, onEvent = {}) }
}

@Preview(name = "Processando ação", showBackground = true)
@Composable
private fun ScheduleDetailContentProcessingPreview() {
    AndroidEventSchedulerTheme {
        ScheduleDetailContent(
            uiState = ScheduleDetailUiState.Content(previewSchedule, isProcessingAction = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Diálogo de exclusão", showBackground = true)
@Composable
private fun ScheduleDetailContentDeleteDialogPreview() {
    AndroidEventSchedulerTheme {
        ScheduleDetailContent(
            uiState = ScheduleDetailUiState.Content(previewSchedule.copy(isRecurring = true), isDeleteDialogVisible = true),
            onEvent = {},
        )
    }
}

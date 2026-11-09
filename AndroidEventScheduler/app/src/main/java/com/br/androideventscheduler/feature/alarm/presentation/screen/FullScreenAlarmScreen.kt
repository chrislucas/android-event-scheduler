package com.br.androideventscheduler.feature.alarm.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.alarm.presentation.component.AlarmActionButtons
import com.br.androideventscheduler.feature.alarm.presentation.component.SnoozeChipsRow
import com.br.androideventscheduler.feature.alarm.presentation.effect.FullScreenAlarmUiEffect
import com.br.androideventscheduler.feature.alarm.presentation.event.FullScreenAlarmUiEvent
import com.br.androideventscheduler.feature.alarm.presentation.state.FullScreenAlarmUiState
import com.br.androideventscheduler.feature.alarm.presentation.viewmodel.FullScreenAlarmViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val SCREEN_PADDING = 24.dp
private val WIDE_LAYOUT_MIN_WIDTH = 600.dp
private val COLOR_STRIPE_WIDTH = 8.dp
private val ROW_ICON_SIZE = 24.dp
private val ROW_MIN_HEIGHT = 48.dp

// Sem preview: injeta o ViewModel (Koin); as previews cobrem o conteúdo stateless FullScreenAlarmContent.
@Composable
internal fun FullScreenAlarmScreen(
    scheduleId: String,
    reloadSignal: Int,
    onClose: (cancelNotification: Boolean) -> Unit,
    viewModel: FullScreenAlarmViewModel = koinViewModel(key = scheduleId) { parametersOf(scheduleId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // `onNewIntent` com a tela aberta: o contador sobe e o ViewModel (chaveado pelo id) recarrega o item.
    LaunchedEffect(reloadSignal, viewModel) {
        if (reloadSignal > 0) viewModel.onEvent(FullScreenAlarmUiEvent.OnReloadRequested)
    }
    BackHandler { viewModel.onEvent(FullScreenAlarmUiEvent.OnBackPressed) }
    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            is FullScreenAlarmUiEffect.CloseScreen -> onClose(effect.cancelNotification)
        }
    }

    FullScreenAlarmContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
internal fun FullScreenAlarmContent(
    uiState: FullScreenAlarmUiState,
    onEvent: (FullScreenAlarmUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState) {
            FullScreenAlarmUiState.Loading -> AlarmLoading()
            is FullScreenAlarmUiState.Content -> AlarmBody(content = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun AlarmLoading() {
    val loadingDescription = stringResource(R.string.state_loading)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .testTag(SemanticsTags.ALARM_FULLSCREEN_LOADING)
                    .semantics { contentDescription = loadingDescription },
        )
    }
}

/**
 * Retrato: coluna única. Largura ≥ 600 dp (paisagem/tablet): informações à esquerda, ações à direita.
 * A ordem de composição é a ordem do TalkBack: selo → título → horário → local → notas → chips → botões.
 */
@Composable
private fun AlarmBody(
    content: FullScreenAlarmUiState.Content,
    onEvent: (FullScreenAlarmUiEvent) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        if (maxWidth >= WIDE_LAYOUT_MIN_WIDTH) {
            Row(
                modifier = Modifier.fillMaxSize().padding(SCREEN_PADDING),
                horizontalArrangement = Arrangement.spacedBy(SCREEN_PADDING),
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AlarmInfo(content)
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    AlarmActions(content, onEvent)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(SCREEN_PADDING),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AlarmInfo(content)
                Spacer(Modifier.height(8.dp))
                AlarmActions(content, onEvent)
            }
        }
    }
}

@Composable
private fun AlarmInfo(content: FullScreenAlarmUiState.Content) {
    AlarmBadge(minutesUntilStart = content.minutesUntilStart)
    Text(
        text = content.title,
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.testTag(SemanticsTags.ALARM_FULLSCREEN_TITLE).semantics { heading() },
    )
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
                        .background(Color(content.colorHex))
                        .clearAndSetSemantics {},
            )
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AlarmInfoRow(
                    iconRes = R.drawable.ic_schedule,
                    text = content.timeText,
                    description = stringResource(R.string.alarm_time_description, content.timeText),
                    tag = SemanticsTags.ALARM_FULLSCREEN_TIME,
                )
                content.location?.let { location ->
                    AlarmInfoRow(
                        iconRes = R.drawable.ic_location,
                        text = location,
                        description = stringResource(R.string.alarm_location_description, location),
                        tag = SemanticsTags.ALARM_FULLSCREEN_LOCATION,
                    )
                }
                content.notes?.let { notes ->
                    AlarmInfoRow(
                        iconRes = R.drawable.ic_notes,
                        text = notes,
                        description = stringResource(R.string.alarm_notes_description, notes),
                        tag = SemanticsTags.ALARM_FULLSCREEN_NOTES,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmActions(
    content: FullScreenAlarmUiState.Content,
    onEvent: (FullScreenAlarmUiEvent) -> Unit,
) {
    SnoozeChipsRow(
        options = content.snoozeOptions,
        selectedMinutes = content.selectedSnoozeMinutes,
        enabled = !content.isProcessing,
        onSelect = { minutes -> onEvent(FullScreenAlarmUiEvent.OnSnoozeDurationSelected(minutes)) },
    )
    content.errorMessageRes?.let { errorRes ->
        Text(
            text = stringResource(errorRes),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(SemanticsTags.ALARM_FULLSCREEN_ERROR).semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    AlarmActionButtons(
        snoozeMinutes = content.selectedSnoozeMinutes,
        enabled = !content.isProcessing,
        onSnooze = { onEvent(FullScreenAlarmUiEvent.OnSnoozeClicked) },
        onDismiss = { onEvent(FullScreenAlarmUiEvent.OnDismissClicked) },
    )
}

/** Selo ciano "Evento em X minutos" (ou "Evento agora" quando X ≤ 0). */
@Composable
private fun AlarmBadge(minutesUntilStart: Int) {
    val badgeText =
        if (minutesUntilStart <= 0) {
            stringResource(R.string.alarm_badge_now)
        } else {
            pluralStringResource(R.plurals.alarm_badge_in_minutes, minutesUntilStart, minutesUntilStart)
        }
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.testTag(SemanticsTags.ALARM_FULLSCREEN_BADGE).padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Linha com ícone decorativo; um único nó semântico lido como "[rótulo]: [texto]". */
@Composable
private fun AlarmInfoRow(
    @DrawableRes iconRes: Int,
    text: String,
    description: String,
    tag: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ROW_MIN_HEIGHT)
                .testTag(tag)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ROW_ICON_SIZE),
        )
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

private val previewContent =
    FullScreenAlarmUiState.Content(
        title = "Aula de Kotlin",
        timeText = "22:00 – 23:30",
        location = null,
        notes = null,
        colorHex = 0xFF5C6BC0,
        minutesUntilStart = 15,
    )

private val previewContentWithDetails =
    previewContent.copy(location = "Sala 3, bloco B", notes = "Levar o notebook e o carregador")

@Preview(name = "Evento simples", showBackground = true)
@Composable
private fun FullScreenAlarmContentSimplePreview() {
    AndroidEventSchedulerTheme { FullScreenAlarmContent(uiState = previewContent, onEvent = {}) }
}

@Preview(name = "Evento simples (claro)", showBackground = true)
@Composable
private fun FullScreenAlarmContentSimpleLightPreview() {
    AndroidEventSchedulerTheme(darkTheme = false) { FullScreenAlarmContent(uiState = previewContent, onEvent = {}) }
}

@Preview(name = "Com local e notas", showBackground = true)
@Composable
private fun FullScreenAlarmContentDetailsPreview() {
    AndroidEventSchedulerTheme { FullScreenAlarmContent(uiState = previewContentWithDetails, onEvent = {}) }
}

@Preview(name = "Evento agora", showBackground = true)
@Composable
private fun FullScreenAlarmContentNowPreview() {
    AndroidEventSchedulerTheme {
        FullScreenAlarmContent(uiState = previewContentWithDetails.copy(minutesUntilStart = 0), onEvent = {})
    }
}

@Preview(name = "Processando", showBackground = true)
@Composable
private fun FullScreenAlarmContentProcessingPreview() {
    AndroidEventSchedulerTheme {
        FullScreenAlarmContent(uiState = previewContentWithDetails.copy(isProcessing = true), onEvent = {})
    }
}

@Preview(name = "Erro ao adiar", showBackground = true)
@Composable
private fun FullScreenAlarmContentErrorPreview() {
    AndroidEventSchedulerTheme {
        FullScreenAlarmContent(
            uiState = previewContentWithDetails.copy(errorMessageRes = R.string.alarm_snooze_error),
            onEvent = {},
        )
    }
}

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun FullScreenAlarmContentLoadingPreview() {
    AndroidEventSchedulerTheme { FullScreenAlarmContent(uiState = FullScreenAlarmUiState.Loading, onEvent = {}) }
}

@Preview(name = "Paisagem", showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun FullScreenAlarmContentLandscapePreview() {
    AndroidEventSchedulerTheme { FullScreenAlarmContent(uiState = previewContentWithDetails, onEvent = {}) }
}

@Preview(name = "Fonte grande", showBackground = true, fontScale = 2f)
@Composable
private fun FullScreenAlarmContentLargeFontPreview() {
    AndroidEventSchedulerTheme { FullScreenAlarmContent(uiState = previewContentWithDetails, onEvent = {}) }
}

package com.br.androideventscheduler.feature.settings.presentation.screen

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.toDisplayLabel
import com.br.androideventscheduler.feature.settings.presentation.component.SettingsNavigationRow
import com.br.androideventscheduler.feature.settings.presentation.component.SettingsRadioGroup
import com.br.androideventscheduler.feature.settings.presentation.component.SettingsSectionTitle
import com.br.androideventscheduler.feature.settings.presentation.component.SettingsSwitchRow
import com.br.androideventscheduler.feature.settings.presentation.effect.SettingsUiEffect
import com.br.androideventscheduler.feature.settings.presentation.event.SettingsUiEvent
import com.br.androideventscheduler.feature.settings.presentation.state.SettingsUiState
import com.br.androideventscheduler.feature.settings.presentation.viewmodel.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

private val SCREEN_HORIZONTAL_PADDING = 16.dp

// Sem preview: injeta o ViewModel (Koin); as previews cobrem o conteúdo stateless SettingsContent.
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            SettingsUiEffect.NavigateBack -> {
                onBack()
            }

            SettingsUiEffect.NavigateToBackup -> {
                onOpenBackup()
            }

            SettingsUiEffect.ShowSavedConfirmation -> {
                Toast.makeText(context, context.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            }

            is SettingsUiEffect.ShowError -> {
                snackbarHostState.showSnackbar(context.getString(effect.messageRes))
            }
        }
    }

    SettingsContent(uiState = uiState, onEvent = viewModel::onEvent, snackbarHostState = snackbarHostState)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(SettingsUiEvent.OnBackClicked) },
                        modifier = Modifier.testTag(SemanticsTags.SETTINGS_BACK_BUTTON),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (uiState) {
            SettingsUiState.Loading -> SettingsLoading(Modifier.padding(innerPadding))
            is SettingsUiState.Content -> SettingsForm(content = uiState, onEvent = onEvent, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun SettingsLoading(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.state_loading)
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .testTag(SemanticsTags.SETTINGS_LOADING)
                    .semantics { contentDescription = loadingDescription },
        )
    }
}

@Composable
private fun SettingsForm(
    content: SettingsUiState.Content,
    onEvent: (SettingsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = content.draft
    val emailDescription = stringResource(R.string.settings_account_email_description)
    val saveDescription = stringResource(R.string.settings_save_description)
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SCREEN_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SettingsSectionTitle(text = stringResource(R.string.settings_appearance_section))
        SettingsRadioGroup(
            options = ThemeMode.entries,
            selectedOption = content.themeMode,
            labelFor = { mode -> stringResource(mode.labelRes()) },
            testTagFor = { mode -> SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + mode.name },
            onSelect = { mode -> onEvent(SettingsUiEvent.AppearanceEvent.OnThemeModeSelected(mode)) },
        )

        SettingsSectionTitle(text = stringResource(R.string.settings_account_section))
        OutlinedTextField(
            value = draft.activeAccountEmail,
            onValueChange = { onEvent(SettingsUiEvent.OnEmailChanged(it)) },
            label = { Text(stringResource(R.string.settings_account_email_label)) },
            isError = content.emailError,
            supportingText =
                if (content.emailError) {
                    { Text(stringResource(R.string.settings_account_email_error)) }
                } else {
                    null
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(SemanticsTags.SETTINGS_ACCOUNT_EMAIL_INPUT)
                    .semantics { contentDescription = emailDescription },
        )

        SettingsSectionTitle(text = stringResource(R.string.settings_reminder_section))
        val resources = LocalContext.current.resources
        SettingsRadioGroup(
            options = ReminderOffset.entries,
            selectedOption = draft.defaultReminderOffset,
            labelFor = { offset -> offset.toDisplayLabel(resources) },
            testTagFor = { offset -> SemanticsTags.SETTINGS_REMINDER_OPTION_PREFIX + offset.name },
            onSelect = { offset -> onEvent(SettingsUiEvent.OnReminderSelected(offset)) },
        )

        SettingsSectionTitle(text = stringResource(R.string.settings_snooze_section))
        SettingsRadioGroup(
            options = UserSettings.ALLOWED_SNOOZE_MINUTES,
            selectedOption = draft.defaultSnoozeMinutes,
            labelFor = { minutes -> pluralStringResource(R.plurals.settings_snooze_minutes, minutes, minutes) },
            testTagFor = { minutes -> SemanticsTags.SETTINGS_SNOOZE_OPTION_PREFIX + minutes },
            onSelect = { minutes -> onEvent(SettingsUiEvent.OnSnoozeSelected(minutes)) },
        )

        Spacer(Modifier.height(8.dp))
        SettingsSwitchRow(
            label = stringResource(R.string.settings_fullscreen_label),
            supportingText = stringResource(R.string.settings_fullscreen_supporting),
            checked = draft.fullScreenEventAlertEnabled,
            onCheckedChange = { onEvent(SettingsUiEvent.OnFullScreenToggled(it)) },
            modifier = Modifier.testTag(SemanticsTags.SETTINGS_FULLSCREEN_SWITCH),
        )

        SettingsSectionTitle(text = stringResource(R.string.settings_backup_section))
        SettingsNavigationRow(
            label = stringResource(R.string.settings_backup_item),
            supportingText = stringResource(R.string.settings_backup_item_supporting),
            onClick = { onEvent(SettingsUiEvent.OnBackupClicked) },
            modifier = Modifier.testTag(SemanticsTags.SETTINGS_BACKUP_ITEM),
        )

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onEvent(SettingsUiEvent.OnSaveClicked) },
            enabled = !content.isSaving,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(SemanticsTags.SETTINGS_SAVE_BUTTON)
                    .semantics { contentDescription = saveDescription },
        ) {
            if (content.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.settings_save_button))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }

private val previewContent = SettingsUiState.Content(draft = UserSettings())

@Preview(name = "Padrão", showBackground = true)
@Composable
private fun SettingsContentPreview() {
    AndroidEventSchedulerTheme { SettingsContent(uiState = previewContent, onEvent = {}) }
}

@Preview(name = "E-mail inválido", showBackground = true)
@Composable
private fun SettingsContentEmailErrorPreview() {
    AndroidEventSchedulerTheme {
        SettingsContent(
            uiState = previewContent.copy(draft = UserSettings(activeAccountEmail = "sem-arroba"), emailError = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Salvando", showBackground = true)
@Composable
private fun SettingsContentSavingPreview() {
    AndroidEventSchedulerTheme { SettingsContent(uiState = previewContent.copy(isSaving = true), onEvent = {}) }
}

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun SettingsContentLoadingPreview() {
    AndroidEventSchedulerTheme { SettingsContent(uiState = SettingsUiState.Loading, onEvent = {}) }
}

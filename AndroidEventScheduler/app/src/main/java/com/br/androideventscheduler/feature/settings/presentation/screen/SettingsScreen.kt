package com.br.androideventscheduler.feature.settings.presentation.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.security.domain.AppLockTimeout
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.domain.DeviceCredentialAuthenticator
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val SCREEN_HORIZONTAL_PADDING = 16.dp

// Sem preview: injeta o ViewModel (Koin); as previews cobrem o conteúdo stateless SettingsContent.
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    authenticator: DeviceCredentialAuthenticator = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val promptTitle = stringResource(R.string.settings_app_lock_prompt_title)
    val promptSubtitle = stringResource(R.string.lock_prompt_subtitle)
    // Escopo da composição, não do ciclo de vida: em API 24–29 a confirmação é outra Activity (esta vai a `onStop`).
    val authenticationScope = rememberCoroutineScope()
    // O usuário pode configurar um PIN nas configurações do Android e voltar.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnScreenResumed) }

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

            // Segurança (spec 16)
            is SettingsUiEffect.AppLockEffect -> {
                when (effect) {
                    SettingsUiEffect.AppLockEffect.RequestAuthentication -> {
                        authenticationScope.launch {
                            val result =
                                activity?.let { authenticator.authenticate(it, promptTitle, promptSubtitle) } ?: AuthResult.NotAvailable
                            viewModel.onEvent(SettingsUiEvent.AppLockEvent.OnAuthResult(result))
                        }
                    }

                    SettingsUiEffect.AppLockEffect.OpenSecuritySettings -> {
                        if (!context.openSecuritySettings()) {
                            snackbarHostState.showSnackbar(context.getString(R.string.settings_app_lock_open_settings_error))
                        }
                    }
                }
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

        // Segurança (spec 16)
        AppLockSection(content = content, onEvent = onEvent)

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

/** Seção Segurança: switch autenticado, aviso de "não é criptografia", tempo e atalho quando o aparelho não tem bloqueio seguro. */
@Composable
private fun AppLockSection(
    content: SettingsUiState.Content,
    onEvent: (SettingsUiEvent) -> Unit,
) {
    val draft = content.draft
    val resources = LocalContext.current.resources
    SettingsSectionTitle(text = stringResource(R.string.settings_security_section))
    SettingsSwitchRow(
        label = stringResource(R.string.settings_app_lock_label),
        supportingText = stringResource(R.string.settings_app_lock_supporting),
        checked = draft.appLockEnabled,
        onCheckedChange = { onEvent(SettingsUiEvent.AppLockEvent.OnToggled(it)) },
        enabled = content.isDeviceSecure,
        modifier = Modifier.testTag(SemanticsTags.SETTINGS_APP_LOCK_SWITCH),
    )
    Text(
        text = stringResource(R.string.settings_app_lock_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(SemanticsTags.SETTINGS_APP_LOCK_DISCLAIMER),
    )
    if (!content.isDeviceSecure) {
        Text(
            text = stringResource(R.string.settings_app_lock_insecure_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(SemanticsTags.SETTINGS_APP_LOCK_INSECURE_HINT),
        )
        OutlinedButton(
            onClick = { onEvent(SettingsUiEvent.AppLockEvent.OnOpenSecuritySettingsClicked) },
            modifier = Modifier.testTag(SemanticsTags.SETTINGS_APP_LOCK_OPEN_SECURITY_BUTTON),
        ) {
            Text(stringResource(R.string.settings_app_lock_open_security))
        }
    }
    if (draft.appLockEnabled) {
        SettingsSectionTitle(text = stringResource(R.string.settings_app_lock_timeout_title))
        SettingsRadioGroup(
            options = AppLockTimeout.entries,
            selectedOption = draft.appLockTimeout,
            labelFor = { timeout -> timeout.toDisplayLabel(resources) },
            testTagFor = { timeout -> SemanticsTags.SETTINGS_APP_LOCK_TIMEOUT_OPTION_PREFIX + timeout.name },
            onSelect = { timeout -> onEvent(SettingsUiEvent.AppLockEvent.OnTimeoutSelected(timeout)) },
        )
    }
}

/** Abre as configurações de segurança do Android; `false` se nenhuma Activity atende (aparelhos customizados). */
private fun Context.openSecuritySettings(): Boolean =
    try {
        startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
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

@Preview(name = "Proteção ligada", showBackground = true)
@Composable
private fun SettingsContentAppLockOnPreview() {
    AndroidEventSchedulerTheme {
        SettingsContent(uiState = previewContent.copy(draft = UserSettings(appLockEnabled = true)), onEvent = {})
    }
}

@Preview(name = "Aparelho sem bloqueio de tela", showBackground = true)
@Composable
private fun SettingsContentDeviceInsecurePreview() {
    AndroidEventSchedulerTheme { SettingsContent(uiState = previewContent.copy(isDeviceSecure = false), onEvent = {}) }
}

@Preview(name = "Carregando", showBackground = true)
@Composable
private fun SettingsContentLoadingPreview() {
    AndroidEventSchedulerTheme { SettingsContent(uiState = SettingsUiState.Loading, onEvent = {}) }
}

package com.br.androideventscheduler.feature.auth.presentation.screen

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.security.domain.AuthResult
import com.br.androideventscheduler.core.security.domain.DeviceCredentialAuthenticator
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.auth.presentation.effect.LockUiEffect
import com.br.androideventscheduler.feature.auth.presentation.event.LockUiEvent
import com.br.androideventscheduler.feature.auth.presentation.state.LockUiState
import com.br.androideventscheduler.feature.auth.presentation.viewmodel.LockViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val SCREEN_PADDING = 32.dp
private val LOCK_ICON_SIZE = 72.dp
private val ELEMENT_SPACING = 20.dp
private val PROGRESS_SIZE = 20.dp

// Sem preview: injeta o ViewModel e o autenticador (Koin) e usa a Activity; as previews cobrem o LockContent stateless.

/**
 * Tela de bloqueio. Não é uma rota do Navigation 3: o gate do `AppNavHost` a mostra no lugar do `NavDisplay`.
 */
@Composable
fun LockScreen(
    modifier: Modifier = Modifier,
    authenticator: DeviceCredentialAuthenticator = koinInject(),
) {
    LockScreenContent(modifier = modifier, authenticator = authenticator, viewModel = koinViewModel())
}

@Composable
internal fun LockScreenContent(
    authenticator: DeviceCredentialAuthenticator,
    viewModel: LockViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? FragmentActivity
    val title = stringResource(R.string.lock_prompt_title)
    val subtitle = stringResource(R.string.lock_prompt_subtitle)
    // Escopo da composição (não do ciclo de vida): em API 24–29 a confirmação é outra Activity, a MainActivity vai a
    // `onStop` e um coletor preso ao ciclo de vida seria cancelado antes de o resultado chegar.
    val authenticationScope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.onEvent(LockUiEvent.OnScreenShown) }
    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            LockUiEffect.LaunchAuthentication -> {
                authenticationScope.launch {
                    val result =
                        try {
                            activity?.let { authenticator.authenticate(it, title, subtitle) } ?: AuthResult.NotAvailable
                        } catch (cancellation: CancellationException) {
                            // Tela recriada no meio da confirmação: libera o botão em vez de deixá-lo preso em "autenticando".
                            viewModel.onEvent(LockUiEvent.OnAuthResult(AuthResult.Canceled))
                            throw cancellation
                        }
                    viewModel.onEvent(LockUiEvent.OnAuthResult(result))
                }
            }
        }
    }

    LockContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}

/** Conteúdo stateless: nenhum dado da agenda, só o cadeado, a explicação e o botão "Desbloquear". */
@Composable
internal fun LockContent(
    uiState: LockUiState,
    onEvent: (LockUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unlockDescription = stringResource(R.string.lock_unlock_description)
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(SCREEN_PADDING)
                .testTag(SemanticsTags.LOCK_SCREEN),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ELEMENT_SPACING),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(LOCK_ICON_SIZE),
            )
            Text(
                text = stringResource(R.string.lock_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.lock_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (uiState is LockUiState.Message) {
                Text(
                    text = stringResource(uiState.messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(SemanticsTags.LOCK_STATUS_MESSAGE),
                )
            }
            val authenticating = uiState is LockUiState.Authenticating
            Button(
                onClick = { onEvent(LockUiEvent.OnUnlockClicked) },
                enabled = !authenticating,
                modifier =
                    Modifier
                        .testTag(SemanticsTags.LOCK_UNLOCK_BUTTON)
                        .semantics { contentDescription = unlockDescription },
            ) {
                if (authenticating) {
                    val authenticatingDescription = stringResource(R.string.lock_authenticating)
                    CircularProgressIndicator(
                        modifier =
                            Modifier
                                .size(PROGRESS_SIZE)
                                .testTag(SemanticsTags.LOCK_PROGRESS)
                                .semantics { contentDescription = authenticatingDescription },
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.lock_unlock_button))
                }
            }
        }
    }
}

@Preview(name = "Inativa", showBackground = true)
@Composable
private fun LockContentIdlePreview() {
    AndroidEventSchedulerTheme { LockContent(uiState = LockUiState.Idle, onEvent = {}) }
}

@Preview(name = "Autenticando", showBackground = true)
@Composable
private fun LockContentAuthenticatingPreview() {
    AndroidEventSchedulerTheme { LockContent(uiState = LockUiState.Authenticating, onEvent = {}) }
}

@Preview(name = "Muitas tentativas", showBackground = true)
@Composable
private fun LockContentLockedOutPreview() {
    AndroidEventSchedulerTheme { LockContent(uiState = LockUiState.Message(R.string.lock_locked_out), onEvent = {}) }
}

@Preview(name = "Indisponível", showBackground = true)
@Composable
private fun LockContentNotAvailablePreview() {
    AndroidEventSchedulerTheme { LockContent(uiState = LockUiState.Message(R.string.lock_not_available), onEvent = {}) }
}

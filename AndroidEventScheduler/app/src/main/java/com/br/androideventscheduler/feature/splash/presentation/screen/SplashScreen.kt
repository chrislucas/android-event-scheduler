package com.br.androideventscheduler.feature.splash.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.splash.presentation.effect.SplashUiEffect
import com.br.androideventscheduler.feature.splash.presentation.event.SplashUiEvent
import com.br.androideventscheduler.feature.splash.presentation.viewmodel.SplashViewModel
import org.koin.compose.viewmodel.koinViewModel

private val LOGO_CIRCLE_SIZE = 112.dp
private val LOGO_ICON_SIZE = 56.dp
private val LOGO_TITLE_SPACING = 24.dp

// Sem preview: injeta o ViewModel (Koin); a preview cobre o conteúdo stateless SplashContent.
@Composable
internal fun SplashScreen(
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(SplashUiEvent.OnSplashStarted) }
    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            SplashUiEffect.NavigateToHome -> onNavigateToHome()
        }
    }
    SplashContent()
}

/** Conteúdo stateless da Splash: marca do RememberMe sobre fundo escuro. */
@Composable
internal fun SplashContent(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LOGO_TITLE_SPACING),
        ) {
            SplashLogo()
            Text(
                text = stringResource(R.string.splash_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .testTag(SemanticsTags.SPLASH_TITLE)
                        .semantics { heading() },
            )
        }
    }
}

@Composable
private fun SplashLogo(modifier: Modifier = Modifier) {
    val logoDescription = stringResource(R.string.splash_logo_description)
    Box(
        modifier =
            modifier
                .size(LOGO_CIRCLE_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .testTag(SemanticsTags.SPLASH_LOGO)
                .semantics(mergeDescendants = true) {
                    role = Role.Image
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bell),
            contentDescription = logoDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(LOGO_ICON_SIZE),
        )
    }
}

@Preview(name = "Splash", showBackground = true)
@Composable
private fun SplashContentPreview() {
    AndroidEventSchedulerTheme {
        SplashContent()
    }
}

@Preview(name = "Logo", showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SplashLogoPreview() {
    AndroidEventSchedulerTheme {
        SplashLogo()
    }
}

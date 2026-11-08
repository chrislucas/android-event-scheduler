package com.br.androideventscheduler.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.navigation.Home
import com.br.androideventscheduler.core.navigation.Splash
import com.br.androideventscheduler.core.navigation.UpsertSchedule
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.detail.navigation.detailEntry
import com.br.androideventscheduler.feature.settings.navigation.settingsEntry
import com.br.androideventscheduler.feature.splash.navigation.splashEntry

// Sem preview: host de navegação que resolve telas e ViewModels pelo Koin; cada tela tem a sua preview.

/**
 * Host de navegação (Navigation 3). Único lugar que conhece todas as features: cada uma contribui com
 * uma extensão `EntryProviderScope<NavKey>.<feature>Entry(...)` registrada no `entryProvider` abaixo.
 * As features nunca enxergam o back stack; recebem lambdas de navegação.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Splash)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                splashEntry(onNavigateToHome = { backStack.replaceSplashWithHome() })
                settingsEntry(onBack = { backStack.removeLastOrNull() })
                // Placeholder temporário; substituído pela feature Home na spec 05.
                entry<Home> { HomePlaceholder() }
                // Detalhes (spec 10)
                detailEntry(
                    onBack = { backStack.removeLastOrNull() },
                    onEdit = { scheduleId -> backStack.add(UpsertSchedule(scheduleId = scheduleId)) },
                )
                // Placeholder temporário; substituído pela feature Upsert na spec 07.
                entry<UpsertSchedule> { UpsertPlaceholder() }
            },
    )
}

@Composable
private fun HomePlaceholder() {
    Scaffold { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag(SemanticsTags.HOME_PLACEHOLDER),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview(showBackground = true, name = "Home placeholder")
@Composable
private fun HomePlaceholderPreview() {
    AndroidEventSchedulerTheme {
        HomePlaceholder()
    }
}

@Composable
private fun UpsertPlaceholder() {
    Scaffold { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag(SemanticsTags.UPSERT_PLACEHOLDER),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.upsert_placeholder_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview(showBackground = true, name = "Upsert placeholder")
@Composable
private fun UpsertPlaceholderPreview() {
    AndroidEventSchedulerTheme {
        UpsertPlaceholder()
    }
}

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
import com.br.androideventscheduler.core.navigation.Splash
import com.br.androideventscheduler.core.ui.SemanticsTags

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
                // Placeholder temporário; substituído pela feature Splash na spec 02.
                entry<Splash> { SplashPlaceholder() }
            },
    )
}

@Composable
private fun SplashPlaceholder() {
    Scaffold { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag(SemanticsTags.SPLASH_PLACEHOLDER),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Preview(showBackground = true, name = "Splash placeholder")
@Composable
private fun SplashPlaceholderPreview() {
    AndroidEventSchedulerTheme {
        SplashPlaceholder()
    }
}

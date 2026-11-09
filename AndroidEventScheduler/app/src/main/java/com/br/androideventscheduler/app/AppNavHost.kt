package com.br.androideventscheduler.app

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.navigation.AgendaBackup
import com.br.androideventscheduler.core.navigation.ScheduleDetail
import com.br.androideventscheduler.core.navigation.Search
import com.br.androideventscheduler.core.navigation.Settings
import com.br.androideventscheduler.core.navigation.Splash
import com.br.androideventscheduler.core.navigation.UpsertSchedule
import com.br.androideventscheduler.core.security.data.AppLockController
import com.br.androideventscheduler.core.security.domain.AppLockState
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.feature.auth.presentation.screen.LockScreen
import com.br.androideventscheduler.feature.backup.navigation.backupEntry
import com.br.androideventscheduler.feature.detail.navigation.detailEntry
import com.br.androideventscheduler.feature.home.navigation.homeEntry
import com.br.androideventscheduler.feature.search.navigation.searchEntry
import com.br.androideventscheduler.feature.settings.navigation.settingsEntry
import com.br.androideventscheduler.feature.splash.navigation.splashEntry
import com.br.androideventscheduler.feature.upsert.navigation.upsertEntry
import org.koin.compose.koinInject

// Sem preview: host de navegação que resolve telas e ViewModels pelo Koin; cada tela tem a sua preview.

/**
 * Host de navegação (Navigation 3). Único lugar que conhece todas as features: cada uma contribui com
 * uma extensão `EntryProviderScope<NavKey>.<feature>Entry(...)` registrada no `entryProvider` abaixo.
 * As features nunca enxergam o back stack; recebem lambdas de navegação.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    appLockController: AppLockController = koinInject(),
) {
    // Fica ACIMA do gate: o back stack sobrevive ao bloqueio e, ao desbloquear, o usuário volta à mesma tela (spec 16).
    val backStack = rememberNavBackStack(Splash)
    val lockState by appLockController.lockState.collectAsStateWithLifecycle()
    LockDisabledWarningEffect(appLockController)
    // Bloqueio do app (spec 16): botão voltar fecha o app e nunca revela o conteúdo.
    val activity = LocalActivity.current
    BackHandler(enabled = lockState != AppLockState.Unlocked) { activity?.finish() }

    when (lockState) {
        // Preferências ainda não lidas: fundo neutro, nada da agenda é composto.
        AppLockState.Pending -> LockPendingBackground(modifier)

        AppLockState.Locked -> LockScreen(modifier)

        AppLockState.Unlocked -> UnlockedNavHost(backStack = backStack, modifier = modifier)
    }
}

/** Aviso único: o bloqueio de tela do celular foi removido e a proteção do app foi desativada (spec 16, R3.6). */
@Composable
private fun LockDisabledWarningEffect(appLockController: AppLockController) {
    val context = LocalContext.current
    CollectEffects(appLockController.lockDisabledBecauseDeviceInsecure) {
        Toast.makeText(context, context.getString(R.string.lock_disabled_device_insecure), Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun LockPendingBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
}

@Composable
private fun UnlockedNavHost(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    // Só compostos desbloqueados: o deep link e o arquivo recebido esperam o desbloqueio e são consumidos depois (R4.2).
    // Deep link da notificação (spec 04): abre o detalhe sobre a Home assim que a Splash termina.
    DeepLinkEffect(backStack = backStack, deepLinkState = koinInject())
    // Arquivo de agenda recebido de outro app (spec 17): abre a pré-visualização da importação.
    IncomingFileEffect(backStack = backStack, incomingFileState = koinInject())

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
                settingsEntry(
                    onBack = { backStack.removeLastOrNull() },
                    // Importar/exportar (spec 17)
                    onOpenBackup = { backStack.add(AgendaBackup()) },
                )
                // Pesquisa (spec 13); o detalhe é empilhado, mantendo a busca no back stack.
                searchEntry(
                    onBack = { backStack.removeLastOrNull() },
                    onOpenDetail = { scheduleId -> backStack.add(ScheduleDetail(scheduleId)) },
                )
                // Home (spec 05)
                homeEntry(
                    onNavigateToSearch = { backStack.add(Search) },
                    onNavigateToUpsert = { dayEpoch -> backStack.add(UpsertSchedule(initialDateEpochDay = dayEpoch)) },
                    onNavigateToDetail = { scheduleId -> backStack.add(ScheduleDetail(scheduleId)) },
                    // Gaveta (spec 06)
                    onNavigateToSettings = { backStack.add(Settings) },
                )
                // Detalhes (spec 10)
                detailEntry(
                    onBack = { backStack.removeLastOrNull() },
                    onEdit = { scheduleId -> backStack.add(UpsertSchedule(scheduleId = scheduleId)) },
                    // Duplicar (spec 11): a cópia substitui o detalhe atual no back stack.
                    onOpenCopy = { copyId ->
                        backStack.removeLastOrNull()
                        backStack.add(ScheduleDetail(copyId))
                    },
                )
                // Criar/editar (spec 07)
                upsertEntry(onBack = { backStack.removeLastOrNull() })

                // Importar/exportar (spec 17)
                backupEntry(onBack = { backStack.removeLastOrNull() })
            },
    )
}

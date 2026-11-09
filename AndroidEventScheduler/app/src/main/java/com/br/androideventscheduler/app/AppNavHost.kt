package com.br.androideventscheduler.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.br.androideventscheduler.core.navigation.AgendaBackup
import com.br.androideventscheduler.core.navigation.ScheduleDetail
import com.br.androideventscheduler.core.navigation.Search
import com.br.androideventscheduler.core.navigation.Settings
import com.br.androideventscheduler.core.navigation.Splash
import com.br.androideventscheduler.core.navigation.UpsertSchedule
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
fun AppNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Splash)
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
                    // Toque duplo / adição rápida (spec 18): não empilhar duas criações (R6.1).
                    onNavigateToUpsert = { dayEpoch ->
                        if (backStack.lastOrNull() !is UpsertSchedule) {
                            backStack.add(UpsertSchedule(initialDateEpochDay = dayEpoch))
                        }
                    },
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

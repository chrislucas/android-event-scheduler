package com.br.androideventscheduler.feature.home.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.permissions.NotificationPermissionEffect
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.presentation.component.CalendarDrawerSheet
import com.br.androideventscheduler.feature.home.presentation.component.MonthGridSection
import com.br.androideventscheduler.feature.home.presentation.component.MonthPickerSheetContent
import com.br.androideventscheduler.feature.home.presentation.component.QuickAddBar
import com.br.androideventscheduler.feature.home.presentation.component.dayScheduleItems
import com.br.androideventscheduler.feature.home.presentation.component.previewContent
import com.br.androideventscheduler.feature.home.presentation.effect.HomeUiEffect
import com.br.androideventscheduler.feature.home.presentation.event.HomeUiEvent
import com.br.androideventscheduler.feature.home.presentation.mapper.toMonthTitle
import com.br.androideventscheduler.feature.home.presentation.state.HomeUiState
import com.br.androideventscheduler.feature.home.presentation.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

// Sem preview: injeta o ViewModel (Koin); as previews cobrem o conteúdo stateless HomeContent.
@Composable
internal fun HomeScreen(
    onNavigateToSearch: () -> Unit,
    onNavigateToUpsert: (initialDateEpochDay: Long) -> Unit,
    onNavigateToDetail: (scheduleId: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Pede POST_NOTIFICATIONS na primeira abertura (spec 04); a tela segue utilizável.
    NotificationPermissionEffect()
    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            HomeUiEffect.NavigateToSearch -> onNavigateToSearch()
            is HomeUiEffect.NavigateToUpsert -> onNavigateToUpsert(effect.initialDateEpochDay)
            is HomeUiEffect.NavigateToDetail -> onNavigateToDetail(effect.scheduleId)
            HomeUiEffect.NavigateToSettings -> onNavigateToSettings()
        }
    }

    HomeContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? HomeUiState.Content
    HomeDrawerHost(content = content, onEvent = onEvent) {
        Scaffold(
            modifier = modifier,
            topBar = { HomeTopBar(content = content, onEvent = onEvent) },
            bottomBar = { QuickAddBar(onClick = { onEvent(HomeUiEvent.OnQuickAddClicked) }) },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (uiState) {
                    HomeUiState.Loading -> HomeLoading()
                    is HomeUiState.Error -> HomeError(messageRes = uiState.messageRes, onRetry = { onEvent(HomeUiEvent.OnRetryClicked) })
                    is HomeUiState.Content -> HomeBody(content = uiState, onEvent = onEvent)
                }
            }
        }
    }
    if (content?.isMonthPickerVisible == true) {
        MonthPickerSheet(content = content, onEvent = onEvent)
    }
}

// Sem preview própria: coberta por HomeContentDrawerOpenPreview (a folha tem as suas em CalendarDrawerSheet).

/**
 * Gaveta lateral. O estado do ViewModel (`isDrawerOpen`) é a fonte da verdade e o [androidx.compose.material3.DrawerState]
 * o segue (abrir/fechar animado). Gestos do usuário (deslizar da borda, deslizar para fechar, toque no scrim) fazem
 * o caminho inverso: quando a gaveta **assenta** num valor diferente do estado, o evento correspondente é enviado.
 * Sem laço: quando a mudança veio do estado, a gaveta assenta no mesmo valor e nada é emitido.
 */
@Composable
private fun HomeDrawerHost(
    content: HomeUiState.Content?,
    onEvent: (HomeUiEvent) -> Unit,
    body: @Composable () -> Unit,
) {
    val isDrawerOpen = content?.isDrawerOpen == true
    val drawerState = rememberDrawerState(if (isDrawerOpen) DrawerValue.Open else DrawerValue.Closed)
    val currentIsDrawerOpen by rememberUpdatedState(isDrawerOpen)
    val currentOnEvent by rememberUpdatedState(onEvent)
    LaunchedEffect(isDrawerOpen) {
        if (isDrawerOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.currentValue == DrawerValue.Open }.collect { settledOpen ->
            when {
                settledOpen && !currentIsDrawerOpen -> currentOnEvent(HomeUiEvent.OnMenuClicked)
                !settledOpen && currentIsDrawerOpen -> currentOnEvent(HomeUiEvent.OnDrawerDismissed)
            }
        }
    }
    // Voltar do sistema com a gaveta aberta: fecha a gaveta (não sai da Home).
    BackHandler(enabled = isDrawerOpen) { onEvent(HomeUiEvent.OnDrawerDismissed) }
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = content != null,
        // A folha é sempre composta (mesmo em Loading/Error, quando fica inacessível): com o conteúdo vazio a
        // largura seria 0, a posição "fechada" seria calculada em 0 e a gaveta apareceria aberta ao chegar o Content.
        drawerContent = {
            CalendarDrawerSheet(
                enabledSources = content?.enabledSources.orEmpty(),
                personalAccountLabel = content?.accountEmail.orEmpty(),
                onToggle = { source -> onEvent(HomeUiEvent.OnCalendarSourceToggled(source)) },
                onSettingsClick = { onEvent(HomeUiEvent.OnSettingsClicked) },
            )
        },
        content = body,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    content: HomeUiState.Content?,
    onEvent: (HomeUiEvent) -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            if (content != null) {
                IconButton(
                    onClick = { onEvent(HomeUiEvent.OnMenuClicked) },
                    modifier = Modifier.testTag(SemanticsTags.HOME_MENU_BUTTON),
                ) {
                    Icon(painter = painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.home_menu_description))
                }
            }
        },
        title = {
            if (content == null) {
                Text(text = stringResource(R.string.splash_title))
            } else {
                val title = content.visibleMonth.toMonthTitle()
                val description = stringResource(R.string.home_month_header_description, title)
                Text(
                    text = title,
                    modifier =
                        Modifier
                            .testTag(SemanticsTags.HOME_MONTH_HEADER)
                            .semantics { contentDescription = description }
                            .clickable(role = Role.Button) { onEvent(HomeUiEvent.OnMonthHeaderClicked) }
                            .padding(vertical = 12.dp),
                )
            }
        },
        actions = {
            if (content != null) {
                IconButton(
                    onClick = { onEvent(HomeUiEvent.OnPreviousMonthClicked) },
                    modifier = Modifier.testTag(SemanticsTags.HOME_PREVIOUS_MONTH_BUTTON),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = stringResource(R.string.home_previous_month),
                    )
                }
                IconButton(
                    onClick = { onEvent(HomeUiEvent.OnNextMonthClicked) },
                    modifier = Modifier.testTag(SemanticsTags.HOME_NEXT_MONTH_BUTTON),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = stringResource(R.string.home_next_month),
                    )
                }
            }
            IconButton(
                onClick = { onEvent(HomeUiEvent.OnSearchClicked) },
                modifier = Modifier.testTag(SemanticsTags.HOME_SEARCH_BUTTON),
            ) {
                Icon(painter = painterResource(R.drawable.ic_search), contentDescription = stringResource(R.string.home_search_description))
            }
        },
    )
}

@Composable
private fun HomeBody(
    content: HomeUiState.Content,
    onEvent: (HomeUiEvent) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(SemanticsTags.HOME_SCHEDULE_LIST)) {
        item(key = "month-grid") {
            MonthGridSection(
                visibleMonth = content.visibleMonth,
                cells = content.cells,
                onDayClick = { date -> onEvent(HomeUiEvent.OnDayClicked(date)) },
                onPreviousMonth = { onEvent(HomeUiEvent.OnPreviousMonthClicked) },
                onNextMonth = { onEvent(HomeUiEvent.OnNextMonthClicked) },
            )
        }
        dayScheduleItems(
            selectedDate = content.selectedDate,
            schedules = content.daySchedules,
            accountEmail = content.accountEmail,
            onScheduleClick = { scheduleId -> onEvent(HomeUiEvent.OnScheduleClicked(scheduleId)) },
        )
    }
}

@Composable
private fun HomeLoading() {
    val loadingDescription = stringResource(R.string.state_loading)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.testTag(SemanticsTags.HOME_LOADING).semantics { contentDescription = loadingDescription },
        )
    }
}

@Composable
private fun HomeError(
    messageRes: Int,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).testTag(SemanticsTags.HOME_ERROR_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = stringResource(messageRes), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp).testTag(SemanticsTags.HOME_RETRY_BUTTON)) {
            Text(text = stringResource(R.string.home_retry))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthPickerSheet(
    content: HomeUiState.Content,
    onEvent: (HomeUiEvent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onEvent(HomeUiEvent.OnMonthPickerDismissed) }) {
        MonthPickerSheetContent(
            year = content.monthPickerYear,
            visibleMonth = content.visibleMonth,
            onYearChange = { delta -> onEvent(HomeUiEvent.OnMonthPickerYearChanged(delta)) },
            onMonthPick = { month -> onEvent(HomeUiEvent.OnMonthPicked(month)) },
        )
    }
}

@Preview(showBackground = true, name = "Com compromissos")
@Composable
private fun HomeContentPreview() {
    AndroidEventSchedulerTheme { HomeContent(uiState = previewContent(), onEvent = {}) }
}

@Preview(showBackground = true, name = "Dia vazio")
@Composable
private fun HomeContentEmptyPreview() {
    AndroidEventSchedulerTheme { HomeContent(uiState = previewContent(daySchedules = emptyList()), onEvent = {}) }
}

@Preview(showBackground = true, name = "Carregando")
@Composable
private fun HomeContentLoadingPreview() {
    AndroidEventSchedulerTheme { HomeContent(uiState = HomeUiState.Loading, onEvent = {}) }
}

@Preview(showBackground = true, name = "Erro")
@Composable
private fun HomeContentErrorPreview() {
    AndroidEventSchedulerTheme { HomeContent(uiState = HomeUiState.Error(R.string.home_error_generic), onEvent = {}) }
}

@Preview(showBackground = true, name = "Gaveta aberta")
@Composable
private fun HomeContentDrawerOpenPreview() {
    AndroidEventSchedulerTheme { HomeContent(uiState = previewContent(isDrawerOpen = true), onEvent = {}) }
}

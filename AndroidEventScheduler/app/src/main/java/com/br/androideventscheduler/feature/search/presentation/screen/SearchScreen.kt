package com.br.androideventscheduler.feature.search.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.CollectEffects
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.search.domain.model.SearchFilter
import com.br.androideventscheduler.feature.search.presentation.component.SearchErrorState
import com.br.androideventscheduler.feature.search.presentation.component.SearchFilterChips
import com.br.androideventscheduler.feature.search.presentation.component.SearchHintState
import com.br.androideventscheduler.feature.search.presentation.component.SearchNoResultsState
import com.br.androideventscheduler.feature.search.presentation.component.SearchResultItem
import com.br.androideventscheduler.feature.search.presentation.effect.SearchUiEffect
import com.br.androideventscheduler.feature.search.presentation.event.SearchUiEvent
import com.br.androideventscheduler.feature.search.presentation.state.SearchResultUi
import com.br.androideventscheduler.feature.search.presentation.state.SearchUiState
import com.br.androideventscheduler.feature.search.presentation.viewmodel.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel

// Sem preview: injeta o ViewModel (Koin); as previews cobrem o conteúdo stateless SearchContent.
@Composable
internal fun SearchScreen(
    onBack: () -> Unit,
    onOpenDetail: (scheduleId: String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CollectEffects(viewModel.uiEffect) { effect ->
        when (effect) {
            SearchUiEffect.NavigateBack -> onBack()
            is SearchUiEffect.NavigateToDetail -> onOpenDetail(effect.scheduleId)
        }
    }

    SearchContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
internal fun SearchContent(
    uiState: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { SearchTopBar(query = uiState.query, onEvent = onEvent) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchFilterChips(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { filter -> onEvent(SearchUiEvent.OnFilterSelected(filter)) },
            )
            SearchBody(uiState = uiState, onEvent = onEvent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onEvent: (SearchUiEvent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val fieldDescription = stringResource(R.string.search_field_description)

    // O campo guarda o próprio valor para o cursor não pular enquanto o estado derivado do ViewModel chega;
    // só mudanças externas (ex.: restauração) sobrescrevem o texto digitado.
    var fieldValue by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    LaunchedEffect(query) {
        if (query != fieldValue.text) fieldValue = TextFieldValue(query, TextRange(query.length))
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    TopAppBar(
        title = {
            TextField(
                value = fieldValue,
                onValueChange = { newValue ->
                    fieldValue = newValue
                    if (newValue.text != query) onEvent(SearchUiEvent.OnQueryChanged(newValue.text))
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag(SemanticsTags.SEARCH_QUERY_INPUT)
                        .semantics { contentDescription = fieldDescription },
                placeholder = { Text(stringResource(R.string.search_field_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                trailingIcon = {
                    if (fieldValue.text.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                fieldValue = TextFieldValue("")
                                onEvent(SearchUiEvent.OnClearQueryClicked)
                                focusRequester.requestFocus()
                            },
                            modifier = Modifier.testTag(SemanticsTags.SEARCH_CLEAR_BUTTON),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.search_clear_description),
                            )
                        }
                    }
                },
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { onEvent(SearchUiEvent.OnBackClicked) },
                modifier = Modifier.testTag(SemanticsTags.SEARCH_BACK_BUTTON),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.navigate_back),
                )
            }
        },
    )
}

@Composable
private fun SearchBody(
    uiState: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
) {
    when (uiState) {
        is SearchUiState.Idle -> {
            SearchHintState()
        }

        is SearchUiState.Empty -> {
            SearchNoResultsState(query = uiState.query.trim())
        }

        is SearchUiState.Error -> {
            SearchErrorState(onRetry = { onEvent(SearchUiEvent.OnRetryClicked) })
        }

        is SearchUiState.Results -> {
            SearchResultsList(results = uiState.results, onEvent = onEvent)
        }

        is SearchUiState.Searching -> {
            SearchingIndicator()
            SearchResultsList(results = uiState.previousResults, onEvent = onEvent)
        }
    }
}

@Composable
private fun SearchingIndicator() {
    val loadingDescription = stringResource(R.string.state_loading)
    LinearProgressIndicator(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(SemanticsTags.SEARCH_LOADING)
                .semantics { contentDescription = loadingDescription },
    )
}

@Composable
private fun SearchResultsList(
    results: List<SearchResultUi>,
    onEvent: (SearchUiEvent) -> Unit,
) {
    if (results.isEmpty()) return
    Text(
        text = pluralStringResource(R.plurals.search_results_count, results.size, results.size),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag(SemanticsTags.SEARCH_RESULTS_COUNT),
    )
    LazyColumn {
        items(items = results, key = { it.id }) { result ->
            SearchResultItem(result = result, onClick = { onEvent(SearchUiEvent.OnResultClicked(result.id)) })
        }
    }
}

private val previewResults =
    listOf(
        SearchResultUi("1", 0xFF26A69A, null, "Reunião de planejamento", "qui., 5 de nov., 10:00", "Evento", "bia@exemplo.com", null),
        SearchResultUi("2", 0xFFAB47BC, "🚀", "Revisar relatório", "sex., 6 de nov., 09:00", "Lembrete", "Meus lembretes", "Diariamente"),
    )

@Preview(showBackground = true, name = "Dica inicial")
@Composable
private fun SearchContentIdlePreview() {
    AndroidEventSchedulerTheme { SearchContent(SearchUiState.Idle("", SearchFilter.ALL), onEvent = {}) }
}

@Preview(showBackground = true, name = "Resultados")
@Composable
private fun SearchContentResultsPreview() {
    AndroidEventSchedulerTheme { SearchContent(SearchUiState.Results("re", SearchFilter.ALL, previewResults), onEvent = {}) }
}

@Preview(showBackground = true, name = "Resultados (claro)")
@Composable
private fun SearchContentResultsLightPreview() {
    AndroidEventSchedulerTheme(darkTheme = false) {
        SearchContent(SearchUiState.Results("re", SearchFilter.ALL, previewResults), onEvent = {})
    }
}

@Preview(showBackground = true, name = "Filtro Eventos")
@Composable
private fun SearchContentEventsFilterPreview() {
    AndroidEventSchedulerTheme {
        SearchContent(SearchUiState.Results("re", SearchFilter.EVENTS, previewResults.take(1)), onEvent = {})
    }
}

@Preview(showBackground = true, name = "Sem resultados")
@Composable
private fun SearchContentEmptyPreview() {
    AndroidEventSchedulerTheme { SearchContent(SearchUiState.Empty("dentista", SearchFilter.ALL), onEvent = {}) }
}

@Preview(showBackground = true, name = "Pesquisando")
@Composable
private fun SearchContentSearchingPreview() {
    AndroidEventSchedulerTheme {
        SearchContent(SearchUiState.Searching("rev", SearchFilter.ALL, previewResults), onEvent = {})
    }
}

@Preview(showBackground = true, name = "Erro")
@Composable
private fun SearchContentErrorPreview() {
    AndroidEventSchedulerTheme { SearchContent(SearchUiState.Error("rev", SearchFilter.ALL), onEvent = {}) }
}

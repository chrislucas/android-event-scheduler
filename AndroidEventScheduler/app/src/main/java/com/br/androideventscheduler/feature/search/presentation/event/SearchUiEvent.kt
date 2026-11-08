package com.br.androideventscheduler.feature.search.presentation.event

import com.br.androideventscheduler.feature.search.domain.model.SearchFilter

internal sealed interface SearchUiEvent {
    data class OnQueryChanged(
        val query: String,
    ) : SearchUiEvent

    data object OnClearQueryClicked : SearchUiEvent

    data class OnFilterSelected(
        val filter: SearchFilter,
    ) : SearchUiEvent

    data class OnResultClicked(
        val scheduleId: String,
    ) : SearchUiEvent

    data object OnRetryClicked : SearchUiEvent

    data object OnBackClicked : SearchUiEvent
}

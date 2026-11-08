package com.br.androideventscheduler.feature.search.presentation.effect

internal sealed interface SearchUiEffect {
    data object NavigateBack : SearchUiEffect

    data class NavigateToDetail(
        val scheduleId: String,
    ) : SearchUiEffect
}

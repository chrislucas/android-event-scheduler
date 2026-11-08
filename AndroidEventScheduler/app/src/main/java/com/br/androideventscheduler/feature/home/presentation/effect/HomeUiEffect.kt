package com.br.androideventscheduler.feature.home.presentation.effect

sealed interface HomeUiEffect {
    data object NavigateToSearch : HomeUiEffect

    data class NavigateToUpsert(
        val initialDateEpochDay: Long,
    ) : HomeUiEffect

    data class NavigateToDetail(
        val scheduleId: String,
    ) : HomeUiEffect

    data object NavigateToSettings : HomeUiEffect
}

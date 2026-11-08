package com.br.androideventscheduler.feature.search.presentation.state

import com.br.androideventscheduler.feature.search.domain.model.SearchFilter

/**
 * Estado derivado de texto + filtro + andamento da consulta. [query] é o texto bruto digitado
 * (sempre atualizado na hora, para o campo ficar responsivo); a consulta usa o texto com `trim`.
 */
internal sealed class SearchUiState {
    abstract val query: String
    abstract val selectedFilter: SearchFilter

    /** Texto em branco: dica "Digite para pesquisar" e nenhuma consulta. */
    data class Idle(
        override val query: String,
        override val selectedFilter: SearchFilter,
    ) : SearchUiState()

    /** Consulta pendente (debounce ou em andamento); mantém os resultados anteriores na tela. */
    data class Searching(
        override val query: String,
        override val selectedFilter: SearchFilter,
        val previousResults: List<SearchResultUi>,
    ) : SearchUiState()

    data class Results(
        override val query: String,
        override val selectedFilter: SearchFilter,
        val results: List<SearchResultUi>,
    ) : SearchUiState()

    data class Empty(
        override val query: String,
        override val selectedFilter: SearchFilter,
    ) : SearchUiState()

    data class Error(
        override val query: String,
        override val selectedFilter: SearchFilter,
    ) : SearchUiState()
}

package com.br.androideventscheduler.feature.search.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.search.domain.model.SearchFilter

/** Grupo exclusivo de filtros (Todos / Eventos / Lembretes); cada chip é um `RadioButton` para leitores de tela. */
@Composable
internal fun SearchFilterChips(
    selectedFilter: SearchFilter,
    onFilterSelected: (SearchFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchFilter.entries.forEach { filter ->
            val description = stringResource(filter.descriptionRes())
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(stringResource(filter.labelRes())) },
                modifier =
                    Modifier
                        .testTag(SemanticsTags.SEARCH_FILTER_CHIP_PREFIX + filter.name)
                        .semantics {
                            role = Role.RadioButton
                            contentDescription = description
                        },
            )
        }
    }
}

@StringRes
private fun SearchFilter.labelRes(): Int =
    when (this) {
        SearchFilter.ALL -> R.string.search_filter_all
        SearchFilter.EVENTS -> R.string.search_filter_events
        SearchFilter.REMINDERS -> R.string.search_filter_reminders
    }

@StringRes
private fun SearchFilter.descriptionRes(): Int =
    when (this) {
        SearchFilter.ALL -> R.string.search_filter_all_description
        SearchFilter.EVENTS -> R.string.search_filter_events_description
        SearchFilter.REMINDERS -> R.string.search_filter_reminders_description
    }

@Preview(showBackground = true, name = "Filtro Todos")
@Composable
private fun SearchFilterChipsAllPreview() {
    AndroidEventSchedulerTheme { SearchFilterChips(selectedFilter = SearchFilter.ALL, onFilterSelected = {}) }
}

@Preview(showBackground = true, name = "Filtro Eventos")
@Composable
private fun SearchFilterChipsEventsPreview() {
    AndroidEventSchedulerTheme { SearchFilterChips(selectedFilter = SearchFilter.EVENTS, onFilterSelected = {}) }
}

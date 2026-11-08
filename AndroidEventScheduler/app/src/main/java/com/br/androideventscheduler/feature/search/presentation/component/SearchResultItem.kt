package com.br.androideventscheduler.feature.search.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.search.presentation.state.SearchResultUi

private val MIN_ROW_HEIGHT = 56.dp
private val COLOR_MARKER_SIZE = 12.dp

/** Linha de resultado; o item inteiro é um único alvo de toque (≥ 48 dp) com semântica mesclada. */
@Composable
internal fun SearchResultItem(
    result: SearchResultUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MIN_ROW_HEIGHT)
                .testTag(SemanticsTags.SEARCH_RESULT_ITEM_PREFIX + result.id)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(COLOR_MARKER_SIZE).background(Color(result.colorHex), CircleShape))
        result.stickerEmoji?.let { emoji -> Text(text = emoji, style = MaterialTheme.typography.titleLarge) }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = result.dateTimeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.search_result_type_and_calendar, result.typeText, result.calendarLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            result.recurrenceText?.let { recurrence ->
                Text(
                    text = recurrence,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private val previewResult =
    SearchResultUi(
        id = "1",
        colorHex = 0xFF26A69A,
        stickerEmoji = null,
        title = "Reunião de planejamento",
        dateTimeText = "qui., 5 de nov., 10:00",
        typeText = "Evento",
        calendarLabel = "bia@exemplo.com",
        recurrenceText = null,
    )

@Preview(showBackground = true, name = "Resultado simples")
@Composable
private fun SearchResultItemPreview() {
    AndroidEventSchedulerTheme { SearchResultItem(result = previewResult, onClick = {}) }
}

@Preview(showBackground = true, name = "Recorrente com adesivo e título longo")
@Composable
private fun SearchResultItemRecurringPreview() {
    AndroidEventSchedulerTheme {
        SearchResultItem(
            result =
                previewResult.copy(
                    stickerEmoji = "🚀",
                    title = "Reunião semanal de alinhamento com toda a equipe de produto e engenharia",
                    recurrenceText = "Toda semana em ter., qui.",
                ),
            onClick = {},
        )
    }
}

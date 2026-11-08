package com.br.androideventscheduler.feature.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.component.StickerButton
import com.br.androideventscheduler.feature.home.presentation.mapper.toDayHeader
import java.time.LocalDate

/** Cabeçalho do dia selecionado (`5 QUI.`) com o botão do adesivo do dia ao lado. */
@Composable
internal fun DayHeader(
    date: LocalDate,
    sticker: String?,
    onStickerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stickerDescription =
        if (sticker == null) {
            stringResource(R.string.home_day_sticker_empty)
        } else {
            stringResource(R.string.home_day_sticker_selected, sticker)
        }
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = date.toDayHeader(),
            modifier =
                Modifier
                    .padding(vertical = 8.dp)
                    .testTag(SemanticsTags.HOME_DAY_HEADER)
                    .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        StickerButton(
            emoji = sticker,
            description = stickerDescription,
            onClick = onStickerClick,
            testTag = SemanticsTags.HOME_DAY_STICKER_BUTTON,
        )
    }
}

@Preview(showBackground = true, name = "Sem adesivo")
@Composable
private fun DayHeaderPreview() {
    AndroidEventSchedulerTheme { DayHeader(date = PreviewDate, sticker = null, onStickerClick = {}) }
}

@Preview(showBackground = true, name = "Com adesivo")
@Composable
private fun DayHeaderStickerPreview() {
    AndroidEventSchedulerTheme { DayHeader(date = PreviewDate, sticker = "🎉", onStickerClick = {}) }
}

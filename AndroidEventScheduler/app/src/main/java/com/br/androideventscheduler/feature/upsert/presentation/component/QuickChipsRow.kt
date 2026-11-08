package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.toTimeLabel
import java.time.LocalTime

/** Linha de atalhos rápidos (quebra de linha automática). Cada chip tem a tag `UPSERT_QUICK_CHIP_PREFIX + id`. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickChipsRow(
    chips: List<QuickChip>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            AssistChip(
                onClick = chip.onClick,
                label = { Text(chip.label) },
                modifier = Modifier.testTag(SemanticsTags.UPSERT_QUICK_CHIP_PREFIX + chip.id),
            )
        }
    }
}

@Preview(name = "Atalhos de data", showBackground = true)
@Composable
private fun QuickChipsRowDatePreview() {
    AndroidEventSchedulerTheme {
        QuickChipsRow(
            chips =
                listOf(
                    QuickChip("today", stringResource(R.string.upsert_chip_today), {}),
                    QuickChip("tomorrow", stringResource(R.string.upsert_chip_tomorrow), {}),
                    QuickChip("in_3_days", stringResource(R.string.upsert_chip_in_3_days), {}),
                    QuickChip("in_1_week", stringResource(R.string.upsert_chip_in_1_week), {}),
                ),
        )
    }
}

@Preview(name = "Atalhos de hora", showBackground = true)
@Composable
private fun QuickChipsRowTimePreview() {
    AndroidEventSchedulerTheme {
        QuickChipsRow(
            chips =
                listOf(
                    QuickChip("in_1_hour", stringResource(R.string.upsert_chip_in_1_hour), {}),
                    QuickChip("at_07_00", LocalTime.of(7, 0).toTimeLabel(), {}),
                    QuickChip("at_15_00", LocalTime.of(15, 0).toTimeLabel(), {}),
                ),
        )
    }
}

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

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
                    QuickChip("today", "Hoje", {}),
                    QuickChip("tomorrow", "Amanhã", {}),
                    QuickChip("in_3_days", "Daqui a 3 dias", {}),
                    QuickChip("in_1_week", "Daqui a 1 semana", {}),
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
                    QuickChip("in_1_hour", "Daqui a 1 hora", {}),
                    QuickChip("at_07_00", "07:00", {}),
                    QuickChip("at_15_00", "15:00", {}),
                ),
        )
    }
}

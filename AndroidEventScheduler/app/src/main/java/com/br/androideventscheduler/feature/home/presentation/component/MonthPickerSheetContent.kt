package com.br.androideventscheduler.feature.home.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.presentation.mapper.toFullName
import com.br.androideventscheduler.feature.home.presentation.mapper.toPickerLabel
import java.time.Month
import java.time.YearMonth

private const val MONTHS_PER_ROW = 3
private val OPTION_MIN_HEIGHT = 48.dp

/** Conteúdo do *bottom sheet* de mês/ano: ano com setas e os 12 meses (`JAN.` … `DEZ.`) em 4 linhas × 3 colunas. */
@Composable
internal fun MonthPickerSheetContent(
    year: Int,
    visibleMonth: YearMonth,
    onYearChange: (delta: Int) -> Unit,
    onMonthPick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag(SemanticsTags.HOME_MONTH_PICKER_SHEET),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(
                onClick = { onYearChange(-1) },
                modifier = Modifier.testTag(SemanticsTags.HOME_PICKER_PREVIOUS_YEAR_BUTTON),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.home_picker_previous_year),
                )
            }
            Text(
                text = stringResource(R.string.home_picker_year, year),
                modifier = Modifier.weight(1f).testTag(SemanticsTags.HOME_PICKER_YEAR).semantics { heading() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
            )
            IconButton(
                onClick = { onYearChange(+1) },
                modifier = Modifier.testTag(SemanticsTags.HOME_PICKER_NEXT_YEAR_BUTTON),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.home_picker_next_year),
                )
            }
        }
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Month.entries.chunked(MONTHS_PER_ROW).forEach { rowMonths ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowMonths.forEach { month ->
                        val option = YearMonth.of(year, month)
                        MonthOption(
                            month = month,
                            year = year,
                            isSelected = option == visibleMonth,
                            onClick = { onMonthPick(option) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthOption(
    month: Month,
    year: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.home_month_option_description, month.toFullName(), year)
    Surface(
        modifier =
            modifier
                .defaultMinSize(minHeight = OPTION_MIN_HEIGHT)
                .testTag(SemanticsTags.HOME_MONTH_OPTION_PREFIX + month.value)
                .semantics { contentDescription = description }
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = month.toPickerLabel(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true, name = "Seletor de mês — novembro selecionado")
@Composable
private fun MonthPickerSheetContentPreview() {
    AndroidEventSchedulerTheme {
        MonthPickerSheetContent(year = 2026, visibleMonth = YearMonth.of(2026, 11), onYearChange = {}, onMonthPick = {})
    }
}

@Preview(showBackground = true, name = "Seletor de mês — outro ano")
@Composable
private fun MonthPickerSheetContentOtherYearPreview() {
    AndroidEventSchedulerTheme {
        MonthPickerSheetContent(year = 2027, visibleMonth = YearMonth.of(2026, 11), onYearChange = {}, onMonthPick = {})
    }
}

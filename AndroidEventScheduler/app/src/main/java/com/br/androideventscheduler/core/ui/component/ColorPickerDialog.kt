package com.br.androideventscheduler.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.labelRes

private val DIALOG_CORNER_RADIUS = 24.dp
private val DIALOG_HORIZONTAL_MARGIN = 16.dp
private val DIALOG_CONTENT_PADDING = 16.dp
private val OPTION_SIZE = 56.dp
private val CHECK_SIZE = 28.dp
private const val OPTIONS_PER_ROW = 3
private const val LIGHT_COLOR_LUMINANCE = 0.5f

/**
 * Seletor de cor (6 cores de [EventColorOption]). A seleção aparece como ícone de check sobre a cor (não só cor)
 * e como estado `selected` do rádio. Escolher chama [onColorSelected] com o `hex`; tocar fora ou voltar chama
 * [onDismiss] sem alterar. [selectedColorHex] fora do catálogo é tratado como a cor padrão.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPickerDialog(
    selectedColorHex: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = EventColorOption.fromHex(selectedColorHex)
    val dialogDescription = stringResource(R.string.color_picker_description)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(DIALOG_CORNER_RADIUS),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                modifier
                    .padding(horizontal = DIALOG_HORIZONTAL_MARGIN)
                    .fillMaxWidth()
                    .semantics { contentDescription = dialogDescription }
                    .testTag(SemanticsTags.COLOR_PICKER_DIALOG),
        ) {
            Column(modifier = Modifier.padding(DIALOG_CONTENT_PADDING), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.color_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    maxItemsInEachRow = OPTIONS_PER_ROW,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EventColorOption.entries.forEach { option ->
                        ColorOption(option = option, isSelected = option == selected, onClick = { onColorSelected(option.hex) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorOption(
    option: EventColorOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(option.labelRes())
    val color = Color(option.hex)
    val ring = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier =
            Modifier
                .size(OPTION_SIZE)
                .clip(CircleShape)
                .background(color)
                .border(BorderStroke(if (isSelected) 3.dp else 1.dp, ring), CircleShape)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label }
                .testTag(SemanticsTags.COLOR_OPTION_PREFIX + option.name),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = if (color.luminance() > LIGHT_COLOR_LUMINANCE) Color.Black else Color.White,
                modifier = Modifier.size(CHECK_SIZE).testTag(SemanticsTags.COLOR_OPTION_CHECK_PREFIX + option.name),
            )
        }
    }
}

@Preview(name = "Azul claro selecionado", showBackground = true)
@Composable
private fun ColorPickerDialogPreview() {
    AndroidEventSchedulerTheme {
        ColorPickerDialog(selectedColorHex = EventColorOption.LIGHT_BLUE.hex, onColorSelected = {}, onDismiss = {})
    }
}

@Preview(name = "Verde oliva selecionado", showBackground = true)
@Composable
private fun ColorPickerDialogOlivePreview() {
    AndroidEventSchedulerTheme {
        ColorPickerDialog(selectedColorHex = EventColorOption.OLIVE_GREEN.hex, onColorSelected = {}, onDismiss = {})
    }
}

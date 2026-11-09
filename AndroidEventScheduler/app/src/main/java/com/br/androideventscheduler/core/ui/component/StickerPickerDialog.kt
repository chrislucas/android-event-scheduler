package com.br.androideventscheduler.core.ui.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.StickerEmoji
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.labelRes

private val DIALOG_CORNER_RADIUS = 24.dp
private val DIALOG_HORIZONTAL_MARGIN = 16.dp
private val DIALOG_CONTENT_PADDING = 16.dp
private val OPTION_SIZE = 56.dp
private const val OPTIONS_PER_ROW = 4

/**
 * Seletor de adesivo (8 emojis de [StickerEmoji]). Escolher chama [onStickerSelected] com o emoji;
 * "Remover adesivo" (só com adesivo selecionado) chama com `null`. Tocar fora ou voltar chama [onDismiss]
 * sem alterar nada. Fechar o diálogo após a escolha é de quem usa (estado de visibilidade).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StickerPickerDialog(
    selectedEmoji: String?,
    onStickerSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dialogDescription = stringResource(R.string.sticker_picker_description)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(DIALOG_CORNER_RADIUS),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                modifier
                    .padding(horizontal = DIALOG_HORIZONTAL_MARGIN)
                    .fillMaxWidth()
                    .semantics { contentDescription = dialogDescription }
                    .testTag(SemanticsTags.STICKER_PICKER_DIALOG),
        ) {
            Column(modifier = Modifier.padding(DIALOG_CONTENT_PADDING), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.sticker_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    maxItemsInEachRow = OPTIONS_PER_ROW,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StickerEmoji.entries.forEach { sticker ->
                        StickerOption(sticker = sticker, isSelected = sticker.emoji == selectedEmoji, onClick = {
                            onStickerSelected(sticker.emoji)
                        })
                    }
                }
                if (selectedEmoji != null) {
                    TextButton(
                        onClick = { onStickerSelected(null) },
                        modifier = Modifier.align(Alignment.End).testTag(SemanticsTags.STICKER_CLEAR_BUTTON),
                    ) {
                        Text(stringResource(R.string.sticker_clear_button))
                    }
                }
            }
        }
    }
}

@Composable
private fun StickerOption(
    sticker: StickerEmoji,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(sticker.labelRes())
    val highlight = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier =
            Modifier
                .size(OPTION_SIZE)
                .clip(CircleShape)
                .border(BorderStroke(if (isSelected) 3.dp else 1.dp, highlight), CircleShape)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label }
                .testTag(SemanticsTags.STICKER_OPTION_PREFIX + sticker.name),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = sticker.emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clearAndSetSemantics {})
    }
}

@Preview(name = "Sem seleção", showBackground = true)
@Composable
private fun StickerPickerDialogPreview() {
    AndroidEventSchedulerTheme { StickerPickerDialog(selectedEmoji = null, onStickerSelected = {}, onDismiss = {}) }
}

@Preview(name = "Com seleção", showBackground = true)
@Composable
private fun StickerPickerDialogSelectedPreview() {
    AndroidEventSchedulerTheme { StickerPickerDialog(selectedEmoji = "🚀", onStickerSelected = {}, onDismiss = {}) }
}

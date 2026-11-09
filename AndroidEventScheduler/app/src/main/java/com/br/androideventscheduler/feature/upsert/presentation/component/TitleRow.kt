package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.component.ColorDotButton
import com.br.androideventscheduler.core.ui.component.StickerButton
import com.br.androideventscheduler.core.ui.format.labelRes

private val BUTTON_TOP_OFFSET = 4.dp

/** Campo de título seguido dos botões de adesivo e de cor (spec 09). */
@Composable
internal fun TitleRow(
    title: String,
    isError: Boolean,
    stickerEmoji: String?,
    colorHex: Long,
    onTitleChange: (String) -> Unit,
    onStickerClick: () -> Unit,
    onColorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stickerDescription =
        if (stickerEmoji == null) {
            stringResource(R.string.upsert_sticker_button_empty)
        } else {
            stringResource(R.string.upsert_sticker_button_selected, stickerEmoji)
        }
    val colorDescription = stringResource(R.string.upsert_color_button, stringResource(EventColorOption.fromHex(colorHex).labelRes()))
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TitleField(title = title, isError = isError, onTitleChange = onTitleChange, modifier = Modifier.weight(1f))
        StickerButton(
            emoji = stickerEmoji,
            description = stickerDescription,
            onClick = onStickerClick,
            testTag = SemanticsTags.UPSERT_STICKER_BUTTON,
            modifier = Modifier.padding(top = BUTTON_TOP_OFFSET),
        )
        ColorDotButton(
            colorHex = EventColorOption.fromHex(colorHex).hex,
            description = colorDescription,
            onClick = onColorClick,
            testTag = SemanticsTags.UPSERT_COLOR_BUTTON,
            modifier = Modifier.padding(top = BUTTON_TOP_OFFSET),
        )
    }
}

@Composable
private fun TitleField(
    title: String,
    isError: Boolean,
    onTitleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldDescription = stringResource(R.string.upsert_title_description)
    val errorMessage = stringResource(R.string.upsert_title_error)
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text(stringResource(R.string.upsert_title_label)) },
        isError = isError,
        supportingText = if (isError) ({ Text(errorMessage) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier =
            modifier
                .semantics {
                    contentDescription = fieldDescription
                    if (isError) error(errorMessage)
                }.testTag(SemanticsTags.UPSERT_TITLE_INPUT),
    )
}

@Composable
private fun PreviewTitleRow(
    title: String = "",
    isError: Boolean = false,
    stickerEmoji: String? = null,
    colorHex: Long = EventColorOption.LIGHT_BLUE.hex,
) {
    AndroidEventSchedulerTheme {
        TitleRow(
            title = title,
            isError = isError,
            stickerEmoji = stickerEmoji,
            colorHex = colorHex,
            onTitleChange = {},
            onStickerClick = {},
            onColorClick = {},
        )
    }
}

@Preview(name = "Vazio", showBackground = true)
@Composable
private fun TitleRowEmptyPreview() {
    PreviewTitleRow()
}

@Preview(name = "Com erro", showBackground = true)
@Composable
private fun TitleRowErrorPreview() {
    PreviewTitleRow(isError = true)
}

@Preview(name = "Com adesivo e cor", showBackground = true)
@Composable
private fun TitleRowAppearancePreview() {
    PreviewTitleRow(title = "Treino", stickerEmoji = "🥋", colorHex = EventColorOption.CORAL.hex)
}

@Preview(name = "Texto longo", showBackground = true)
@Composable
private fun TitleRowLongPreview() {
    PreviewTitleRow(title = "Reunião de planejamento trimestral com todas as equipes de produto, design e engenharia")
}

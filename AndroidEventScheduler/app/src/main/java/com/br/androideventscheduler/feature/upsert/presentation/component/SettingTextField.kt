package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

/** Campo de texto livre do formulário (local: uma linha; notas: multilinha). */
@Composable
private fun SettingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    @StringRes descriptionRes: Int,
    tag: String,
    isSingleLine: Boolean,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(descriptionRes)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = isSingleLine,
        minLines = if (isSingleLine) 1 else NOTES_MIN_LINES,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = description }
                .testTag(tag),
    )
}

private const val NOTES_MIN_LINES = 3

@Composable
internal fun LocationField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingTextField(
        value = value,
        onValueChange = onValueChange,
        labelRes = R.string.upsert_location_label,
        descriptionRes = R.string.upsert_location_description,
        tag = SemanticsTags.UPSERT_LOCATION_INPUT,
        isSingleLine = true,
        imeAction = ImeAction.Next,
        modifier = modifier,
    )
}

@Composable
internal fun NotesField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingTextField(
        value = value,
        onValueChange = onValueChange,
        labelRes = R.string.upsert_notes_label,
        descriptionRes = R.string.upsert_notes_description,
        tag = SemanticsTags.UPSERT_NOTES_INPUT,
        isSingleLine = false,
        imeAction = ImeAction.Default,
        modifier = modifier,
    )
}

@Preview(name = "Local", showBackground = true)
@Composable
private fun LocationFieldPreview() {
    AndroidEventSchedulerTheme { LocationField(value = "Sala 3, bloco B", onValueChange = {}) }
}

@Preview(name = "Notas multilinha", showBackground = true)
@Composable
private fun NotesFieldPreview() {
    AndroidEventSchedulerTheme { NotesField(value = "Levar o notebook\nConfirmar a sala", onValueChange = {}) }
}

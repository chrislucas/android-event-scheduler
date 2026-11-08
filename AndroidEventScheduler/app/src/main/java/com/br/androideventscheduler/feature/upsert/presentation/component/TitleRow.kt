package com.br.androideventscheduler.feature.upsert.presentation.component

import androidx.compose.foundation.layout.fillMaxWidth
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
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

/** Campo de título. As specs 08/09 acrescentam a esta linha os botões de emoji e cor. */
@Composable
internal fun TitleRow(
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
                .fillMaxWidth()
                .semantics {
                    contentDescription = fieldDescription
                    if (isError) error(errorMessage)
                }.testTag(SemanticsTags.UPSERT_TITLE_INPUT),
    )
}

@Preview(name = "Vazio", showBackground = true)
@Composable
private fun TitleRowEmptyPreview() {
    AndroidEventSchedulerTheme { TitleRow(title = "", isError = false, onTitleChange = {}) }
}

@Preview(name = "Com erro", showBackground = true)
@Composable
private fun TitleRowErrorPreview() {
    AndroidEventSchedulerTheme { TitleRow(title = "", isError = true, onTitleChange = {}) }
}

@Preview(name = "Texto longo", showBackground = true)
@Composable
private fun TitleRowLongPreview() {
    AndroidEventSchedulerTheme {
        TitleRow(
            title = "Reunião de planejamento trimestral com todas as equipes de produto, design e engenharia",
            isError = false,
            onTitleChange = {},
        )
    }
}

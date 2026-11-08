package com.br.androideventscheduler.feature.detail.presentation.component

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

private val BUTTON_MIN_HEIGHT = 48.dp

/** Confirmação de exclusão com o título do item; para recorrentes avisa que **todas** as ocorrências serão removidas. */
@Composable
internal fun DeleteConfirmationDialog(
    title: String,
    isRecurring: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dialogDescription = stringResource(R.string.delete_dialog_description)
    val messageRes = if (isRecurring) R.string.delete_dialog_message_recurring else R.string.delete_dialog_message
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier =
            modifier
                .testTag(SemanticsTags.DELETE_CONFIRM_DIALOG)
                .semantics { contentDescription = dialogDescription },
        title = { Text(text = stringResource(R.string.delete_dialog_title, title)) },
        text = { Text(text = stringResource(messageRes)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.defaultMinSize(minHeight = BUTTON_MIN_HEIGHT).testTag(SemanticsTags.DELETE_CONFIRM_BUTTON),
            ) {
                Text(text = stringResource(R.string.delete_dialog_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = BUTTON_MIN_HEIGHT).testTag(SemanticsTags.DELETE_CANCEL_BUTTON),
            ) {
                Text(text = stringResource(R.string.delete_dialog_cancel))
            }
        },
    )
}

@Preview(name = "Item simples", showBackground = true)
@Composable
private fun DeleteConfirmationDialogPreview() {
    AndroidEventSchedulerTheme {
        DeleteConfirmationDialog(title = "Aula de Kotlin", isRecurring = false, onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "Item recorrente", showBackground = true)
@Composable
private fun DeleteConfirmationDialogRecurringPreview() {
    AndroidEventSchedulerTheme {
        DeleteConfirmationDialog(title = "Academia", isRecurring = true, onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "Título longo", showBackground = true)
@Composable
private fun DeleteConfirmationDialogLongTitlePreview() {
    AndroidEventSchedulerTheme {
        DeleteConfirmationDialog(
            title = "Workshop de arquitetura Android com Compose, Koin, Navigation 3 e testes automatizados de ponta a ponta",
            isRecurring = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

package com.br.androideventscheduler.feature.detail.presentation.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

private val ACTION_MIN_SIZE = 48.dp
private val ACTION_ICON_SIZE = 24.dp
private const val DISABLED_ALPHA = 0.38f

/**
 * Uma ação da barra inferior. [label] é o texto visível; [description] é o que o leitor de tela anuncia
 * (por padrão, o próprio [label]).
 */
internal data class ScheduleAction(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val tag: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    @param:StringRes val description: Int = label,
    /** Ação destrutiva (ex.: Excluir): ícone e rótulo na cor de erro. */
    val isDestructive: Boolean = false,
)

/** Barra de ações em pílula. A spec 12 só acrescenta itens à lista (Duplicar, Editar, Compartilhar, Excluir). */
@Composable
internal fun ScheduleActionBar(
    actions: List<ScheduleAction>,
    modifier: Modifier = Modifier,
) {
    val barDescription = stringResource(R.string.detail_action_bar_description)
    Surface(
        modifier =
            modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .testTag(SemanticsTags.DETAIL_ACTION_BAR)
                .semantics { contentDescription = barDescription },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { action -> ScheduleActionButton(action = action, modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun ScheduleActionButton(
    action: ScheduleAction,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(action.description)
    val contentColor = if (action.isDestructive) MaterialTheme.colorScheme.error else LocalContentColor.current
    Column(
        modifier =
            modifier
                .defaultMinSize(minWidth = ACTION_MIN_SIZE, minHeight = ACTION_MIN_SIZE)
                .alpha(if (action.enabled) 1f else DISABLED_ALPHA)
                .testTag(action.tag)
                .clickable(enabled = action.enabled, role = Role.Button, onClick = action.onClick)
                .semantics { contentDescription = description }
                .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(action.icon),
            contentDescription = null,
            modifier = Modifier.size(ACTION_ICON_SIZE),
            tint = contentColor,
        )
        Text(
            text = stringResource(action.label),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            // Em telas estreitas o rótulo pode virar "Compart…"; a descrição de acessibilidade não muda.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val previewEditAction =
    ScheduleAction(
        label = R.string.detail_edit_label,
        icon = R.drawable.ic_edit,
        tag = SemanticsTags.DETAIL_EDIT_BUTTON,
        onClick = {},
        description = R.string.detail_edit_description,
    )

private val previewDuplicateAction =
    ScheduleAction(
        label = R.string.detail_duplicate_label,
        icon = R.drawable.ic_content_copy,
        tag = SemanticsTags.DETAIL_DUPLICATE_BUTTON,
        onClick = {},
        description = R.string.detail_duplicate_description,
    )

private val previewShareAction =
    ScheduleAction(
        label = R.string.detail_share_label,
        icon = R.drawable.ic_share,
        tag = SemanticsTags.DETAIL_SHARE_BUTTON,
        onClick = {},
        description = R.string.detail_share_description,
    )

private val previewDeleteAction =
    ScheduleAction(
        label = R.string.detail_delete_label,
        icon = R.drawable.ic_delete,
        tag = SemanticsTags.DETAIL_DELETE_BUTTON,
        onClick = {},
        description = R.string.detail_delete_description,
        isDestructive = true,
    )

@Preview(name = "Só Editar", showBackground = true)
@Composable
private fun ScheduleActionBarPreview() {
    AndroidEventSchedulerTheme { ScheduleActionBar(actions = listOf(previewEditAction)) }
}

@Preview(name = "Várias ações, uma desabilitada", showBackground = true)
@Composable
private fun ScheduleActionBarManyActionsPreview() {
    AndroidEventSchedulerTheme {
        ScheduleActionBar(
            actions =
                listOf(
                    previewEditAction.copy(tag = "preview_a"),
                    previewEditAction.copy(tag = "preview_b", enabled = false),
                    previewEditAction.copy(tag = "preview_c"),
                ),
        )
    }
}

@Preview(name = "Barra completa", showBackground = true)
@Composable
private fun ScheduleActionBarFullPreview() {
    AndroidEventSchedulerTheme {
        ScheduleActionBar(
            actions = listOf(previewDuplicateAction, previewEditAction, previewShareAction, previewDeleteAction),
        )
    }
}

@Preview(name = "Duplicar, Editar e Excluir", showBackground = true)
@Composable
private fun ScheduleActionBarDuplicateEditDeletePreview() {
    AndroidEventSchedulerTheme {
        ScheduleActionBar(actions = listOf(previewDuplicateAction, previewEditAction, previewDeleteAction))
    }
}

@Preview(name = "Processando (desabilitadas)", showBackground = true)
@Composable
private fun ScheduleActionBarProcessingPreview() {
    AndroidEventSchedulerTheme {
        ScheduleActionBar(
            actions =
                listOf(previewDuplicateAction, previewEditAction, previewShareAction, previewDeleteAction)
                    .map { it.copy(enabled = false) },
        )
    }
}

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
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    val tag: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    @StringRes val description: Int = label,
)

/** Barra de ações em pílula. As specs 11 e 12 só acrescentam itens à lista (Duplicar, Editar, Compartilhar, Excluir). */
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
        )
        Text(text = stringResource(action.label), style = MaterialTheme.typography.labelMedium)
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

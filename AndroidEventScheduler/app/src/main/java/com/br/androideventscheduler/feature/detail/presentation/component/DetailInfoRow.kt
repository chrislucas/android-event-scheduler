package com.br.androideventscheduler.feature.detail.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

private val ROW_MIN_HEIGHT = 48.dp
private val LEADING_SIZE = 24.dp
private val CALENDAR_DOT_SIZE = 14.dp

/**
 * Linha de informação do detalhe: marcador à esquerda + [text]. É um único nó semântico (`mergeDescendants`)
 * lido como "[label]: [text]" (ex.: "Alerta: 10 minutos antes"); [leading] é decorativo.
 */
@Composable
internal fun DetailInfoRow(
    label: String,
    text: String,
    tag: String,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    val description = stringResource(R.string.detail_row_description, label, text)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ROW_MIN_HEIGHT)
                .testTag(tag)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.size(LEADING_SIZE), contentAlignment = Alignment.Center) { leading() }
        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

/** Ícone decorativo (o significado vem do rótulo da linha). */
@Composable
internal fun DetailRowIcon(
    @DrawableRes iconRes: Int,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(LEADING_SIZE),
    )
}

/** Ponto na cor do calendário; decorativo. */
@Composable
internal fun DetailColorDot(colorHex: Long) {
    Box(
        modifier =
            Modifier
                .size(CALENDAR_DOT_SIZE)
                .clip(CircleShape)
                .background(Color(colorHex))
                .clearAndSetSemantics {},
    )
}

@Preview(name = "Linha com ícone", showBackground = true)
@Composable
private fun DetailInfoRowPreview() {
    AndroidEventSchedulerTheme {
        DetailInfoRow(
            label = stringResource(R.string.detail_label_reminder),
            text = "10 minutos antes",
            tag = SemanticsTags.DETAIL_REMINDER_LABEL,
            leading = { DetailRowIcon(R.drawable.ic_bell) },
        )
    }
}

@Preview(name = "Linha com ponto do calendário", showBackground = true)
@Composable
private fun DetailInfoRowCalendarPreview() {
    AndroidEventSchedulerTheme {
        DetailInfoRow(
            label = stringResource(R.string.detail_label_calendar),
            text = "christoffer.luccas@gmail.com",
            tag = SemanticsTags.DETAIL_ACCOUNT_EMAIL,
            leading = { DetailColorDot(colorHex = 0xFF29B6F6) },
        )
    }
}

@Preview(name = "Linha com texto longo", showBackground = true)
@Composable
private fun DetailInfoRowLongTextPreview() {
    AndroidEventSchedulerTheme {
        DetailInfoRow(
            label = stringResource(R.string.detail_label_notes),
            text = "Levar o notebook, o carregador, os documentos impressos e lembrar de confirmar a sala com a recepção antes de sair.",
            tag = SemanticsTags.DETAIL_NOTES,
            leading = { DetailRowIcon(R.drawable.ic_notes) },
        )
    }
}

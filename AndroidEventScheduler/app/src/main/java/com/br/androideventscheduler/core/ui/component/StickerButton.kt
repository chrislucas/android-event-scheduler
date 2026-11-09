package com.br.androideventscheduler.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme

private val TOUCH_TARGET = 48.dp

/**
 * Botão circular do adesivo: mostra o [emoji] ou, sem adesivo, o ícone de rosto. Alvo de toque de 48 dp.
 * [description] vem de quem usa (ex.: `Adesivo selecionado: 🚀. Toque para alterar`).
 */
@Composable
internal fun StickerButton(
    emoji: String?,
    description: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(TOUCH_TARGET)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
                .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji != null) {
            Text(text = emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
        } else {
            Icon(painter = painterResource(R.drawable.ic_sticker), contentDescription = null, tint = LocalContentColor.current)
        }
    }
}

@Preview(name = "Sem adesivo", showBackground = true)
@Composable
private fun StickerButtonEmptyPreview() {
    AndroidEventSchedulerTheme {
        StickerButton(emoji = null, description = "Selecionar emoji ou adesivo", onClick = {}, testTag = "preview")
    }
}

@Preview(name = "Com adesivo", showBackground = true)
@Composable
private fun StickerButtonSelectedPreview() {
    AndroidEventSchedulerTheme {
        Row(modifier = Modifier.padding(8.dp)) {
            StickerButton(emoji = "🚀", description = "Adesivo selecionado: 🚀. Toque para alterar", onClick = {}, testTag = "preview")
        }
    }
}

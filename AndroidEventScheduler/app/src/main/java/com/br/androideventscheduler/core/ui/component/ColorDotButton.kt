package com.br.androideventscheduler.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme

private val TOUCH_TARGET = 48.dp
private val DOT_SIZE = 28.dp

/**
 * Botão circular da cor: círculo de 28 dp dentro de um alvo de toque de 48 dp. A cor não é a única pista:
 * [description] (ex.: `Cor selecionada: Azul claro. Toque para alterar`) é fornecida por quem usa.
 */
@Composable
internal fun ColorDotButton(
    colorHex: Long,
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
        Box(
            modifier =
                Modifier
                    .size(DOT_SIZE)
                    .clip(CircleShape)
                    .background(Color(colorHex))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), CircleShape),
        )
    }
}

@Preview(name = "Azul claro", showBackground = true)
@Composable
private fun ColorDotButtonPreview() {
    AndroidEventSchedulerTheme {
        ColorDotButton(
            colorHex = 0xFF29B6F6,
            description = "Cor selecionada: Azul claro. Toque para alterar",
            onClick = {},
            testTag = "preview",
        )
    }
}

@Preview(name = "Coral", showBackground = true)
@Composable
private fun ColorDotButtonCoralPreview() {
    AndroidEventSchedulerTheme {
        ColorDotButton(colorHex = 0xFFEF5350, description = "Cor selecionada: Coral. Toque para alterar", onClick = {}, testTag = "preview")
    }
}

package com.br.androideventscheduler.core.ui.theme

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import android.graphics.Color as AndroidColor

/**
 * Faz a janela acompanhar o tema (spec 19, R3.3), fora da composição de pixels:
 * - ícones das barras de status e de navegação com contraste: claros no escuro, escuros no claro ([SystemBarStyle]);
 * - fundo da janela igual ao `background` do tema, para não aparecer faixa da outra cor em transições e no teclado.
 *
 * Chamar `enableEdgeToEdge` de novo só troca o estilo das barras; a Activity não é recriada. Sem `ComponentActivity`
 * (previews, testes de composição soltos) não faz nada.
 */
@Composable
fun SystemThemeEffects(darkTheme: Boolean) {
    val activity = LocalContext.current.findComponentActivity() ?: return
    val windowBackground = MaterialTheme.colorScheme.background.toArgb()
    DisposableEffect(activity, darkTheme, windowBackground) {
        val barStyle =
            if (darkTheme) {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            }
        activity.enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        activity.window.setBackgroundDrawable(windowBackground.toDrawable())
        onDispose { }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }

// Conteúdo de exemplo: o efeito em si não desenha nada; a preview mostra as duas paletas que ele acompanha.
@PreviewLightDark
@Composable
private fun SystemThemeEffectsPreview() {
    AndroidEventSchedulerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.theme_preview_background_text), color = MaterialTheme.colorScheme.onBackground)
                Text(text = stringResource(R.string.theme_preview_secondary_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = stringResource(R.string.theme_preview_accent_text), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

package com.br.androideventscheduler.core.ui.format

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.schedule.domain.model.StickerEmoji
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class AppearanceFormatTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `given the sticker catalog when reading labels then each emoji has its pt-BR label`() {
        val labels = StickerEmoji.entries.map { context.getString(it.labelRes()) }

        assertEquals(
            listOf(
                "Rosto sorridente",
                "Livros de estudo",
                "Computador e programação",
                "Foguete",
                "Estrela",
                "Comemoração",
                "Treino",
                "Pausa para café",
            ),
            labels,
        )
    }

    @Test
    fun `given the color catalog when reading labels then each color has its pt-BR name`() {
        val labels = EventColorOption.entries.map { context.getString(it.labelRes()) }

        assertEquals(listOf("Azul claro", "Índigo", "Verde-azulado", "Verde oliva", "Roxo", "Coral"), labels)
    }
}

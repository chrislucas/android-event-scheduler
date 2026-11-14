package com.br.androideventscheduler

import android.os.Looper
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.schedule.domain.model.StickerEmoji
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.domain.repository.HomePreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Ponta a ponta (specs 05 + 07 + 09) com a `MainActivity` e o grafo real: criar um item escolhendo cor e adesivo no
 * formulário → a Home mostra a cor (indicador do dia e barra da lista) e o adesivo do item; depois o adesivo do dia
 * é escolhido no cabeçalho e fica gravado no DataStore para o dia selecionado.
 */
@RunWith(RobolectricTestRunner::class)
// Tela alta: o formulário de criação e a agenda do dia ficam inteiros na área visível.
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class ColorAndEmojiShowOnHomeRobolectricTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    private fun hasDescription(
        tag: String,
        description: String,
    ) = composeRule
        .onAllNodes(hasTestTag(tag) and SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(description)))
        .fetchSemanticsNodes()
        .isNotEmpty()

    private fun click(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `given the form when choosing color and emoji and saving then the home shows both and the day sticker persists`() {
        val title = "Judô ponta a ponta"
        val today = LocalDate.now()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_QUICK_ADD_BAR) }
            click(SemanticsTags.HOME_QUICK_ADD_BAR)

            waitUntilWithMainLooper { hasNode(SemanticsTags.UPSERT_TITLE_INPUT) }
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).performTextInput(title)
            click(SemanticsTags.UPSERT_STICKER_BUTTON)
            click(SemanticsTags.STICKER_OPTION_PREFIX + StickerEmoji.WORKOUT.name)
            click(SemanticsTags.UPSERT_COLOR_BUTTON)
            click(SemanticsTags.COLOR_OPTION_PREFIX + EventColorOption.CORAL.name)
            click(SemanticsTags.UPSERT_SAVE_BUTTON)

            val barTag = { id: String -> "${SemanticsTags.HOME_SCHEDULE_COLOR_BAR_PREFIX}${id}_${EventColorOption.CORAL.hex}" }
            val repository = GlobalContext.get().get<ScheduleRepository>()
            waitUntilWithMainLooper {
                !hasNode(SemanticsTags.UPSERT_TITLE_INPUT) && hasNode(barTag(runBlocking { repository.getAll() }.single().id))
            }

            val stored = runBlocking { repository.getAll() }.single()
            assertEquals(title, stored.title)
            assertEquals(EventColorOption.CORAL.hex, stored.colorHex)
            assertEquals(StickerEmoji.WORKOUT.emoji, stored.stickerEmoji)
            assertEquals(true, hasNode("${SemanticsTags.HOME_DAY_INDICATOR_PREFIX}${today.toEpochDay()}_${EventColorOption.CORAL.hex}"))
            composeRule
                .onNode(
                    hasText(StickerEmoji.WORKOUT.emoji) and hasAnyAncestor(hasTestTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + stored.id)),
                    useUnmergedTree = true,
                ).assertExists()

            // Parte Home da spec: adesivo do dia selecionado, persistido por epochDay.
            click(SemanticsTags.HOME_DAY_STICKER_BUTTON)
            click(SemanticsTags.STICKER_OPTION_PREFIX + StickerEmoji.PARTY.name)
            val preferences = GlobalContext.get().get<HomePreferencesRepository>()
            // A gravação no DataStore roda fora do looper principal: espera o valor persistir e a UI refletir.
            val expectedDescription = "Adesivo do dia: ${StickerEmoji.PARTY.emoji}. Toque para alterar"
            waitUntilWithMainLooper {
                runBlocking { preferences.observeDaySticker(today.toEpochDay()).first() } == StickerEmoji.PARTY.emoji &&
                    hasDescription(SemanticsTags.HOME_DAY_STICKER_BUTTON, expectedDescription)
            }
            composeRule.onNodeWithTag(SemanticsTags.HOME_DAY_STICKER_BUTTON).assertContentDescriptionEquals(expectedDescription)
        }
    }
}

package com.br.androideventscheduler.feature.search

import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.search.presentation.screen.SearchScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Ponta a ponta com o grafo real: tela → ViewModel (Koin, dispatcher main) → repositório → Room.
 * O resultado do Room retoma na main via `Handler.post`, e o debounce usa o relógio do looper: no modo
 * PAUSED do Robolectric ambos só avançam quando o looper é drenado/avançado dentro do `waitUntil`.
 */
@RunWith(RobolectricTestRunner::class)
class SearchFlowRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `given a saved item when typing an unaccented query then the result shows and clicking opens its detail`() {
        val repository = GlobalContext.get().get<ScheduleRepository>()
        runBlocking { repository.upsert(scheduleItem(id = "flow-1", title = "REUNIÃO final")) }
        var openedDetailId: String? = null
        composeRule.setContent {
            AndroidEventSchedulerTheme { SearchScreen(onBack = {}, onOpenDetail = { openedDetailId = it }) }
        }

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_QUERY_INPUT).performTextReplacement("reuniao")
        waitUntilWithMainLooper { hasNode(SemanticsTags.SEARCH_RESULT_ITEM_PREFIX + "flow-1") }
        composeRule.onNodeWithTag(SemanticsTags.SEARCH_RESULT_ITEM_PREFIX + "flow-1").performClick()
        waitUntilWithMainLooper { openedDetailId != null }

        assertEquals("flow-1", openedDetailId)
    }

    @Test
    fun `given a query without matches when it settles then the empty state is shown and back closes`() {
        var backCalls = 0
        composeRule.setContent {
            AndroidEventSchedulerTheme { SearchScreen(onBack = { backCalls++ }, onOpenDetail = {}) }
        }

        composeRule.onNodeWithTag(SemanticsTags.SEARCH_QUERY_INPUT).performTextReplacement("zzzinexistente")
        waitUntilWithMainLooper { hasNode(SemanticsTags.SEARCH_EMPTY_STATE) }
        composeRule.onNodeWithTag(SemanticsTags.SEARCH_BACK_BUTTON).performClick()
        waitUntilWithMainLooper { backCalls == 1 }

        assertEquals(1, backCalls)
    }
}

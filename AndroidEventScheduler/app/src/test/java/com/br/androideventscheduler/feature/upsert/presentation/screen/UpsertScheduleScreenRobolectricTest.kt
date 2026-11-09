package com.br.androideventscheduler.feature.upsert.presentation.screen

import android.app.AlarmManager
import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.CalendarSource
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.schedule.domain.model.ScheduleType
import com.br.androideventscheduler.core.schedule.domain.model.StickerEmoji
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.core.ui.format.toShortDateLabel
import com.br.androideventscheduler.core.ui.format.toTimeLabel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowDialog
import java.time.LocalDate
import java.time.LocalTime
import java.util.TimeZone

/**
 * Tela completa com o grafo real: tela → ViewModel (Koin, dispatcher main) → `SaveScheduleUseCase` → Room em
 * memória + `AlarmManager` (shadow). O `idle()` drena o looper principal, onde o resultado de I/O retoma.
 */
@RunWith(RobolectricTestRunner::class)
// Tela alta: a LazyColumn só compõe os itens visíveis, e o formulário com seletor aberto é longo.
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class UpsertScheduleScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository: ScheduleRepository get() = GlobalContext.get().get()
    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private lateinit var originalTimeZone: TimeZone
    private var backCalls = 0

    // Dia 10 de dois meses à frente: sempre futuro, e o calendário mostra o dia 20 do mesmo mês.
    private val routeDate: LocalDate = LocalDate.now().plusMonths(2).withDayOfMonth(10)

    @Before
    fun useNegativeOffsetTimeZone() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
    }

    @After
    fun restoreEnvironment() {
        TimeZone.setDefault(originalTimeZone)
        ShadowAlarmManager.reset()
    }

    private fun showScreen(
        scheduleId: String? = null,
        initialDate: LocalDate? = routeDate,
    ) {
        backCalls = 0
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                UpsertScheduleScreen(
                    scheduleId = scheduleId,
                    initialDateEpochDay = initialDate?.toEpochDay(),
                    onBack = { backCalls++ },
                )
            }
        }
        waitUntilWithMainLooper { tagCount(SemanticsTags.UPSERT_TITLE_INPUT) > 0 }
    }

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun tagCount(tag: String): Int = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    private fun click(tag: String) {
        composeRule.onNodeWithTag(tag).performClick()
        composeRule.waitForIdle()
    }

    private fun typeTitle(title: String) {
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).performTextInput(title)
    }

    private fun expandedState() = context.getString(R.string.upsert_state_expanded)

    private fun collapsedState() = context.getString(R.string.upsert_state_collapsed)

    // region modos

    @Test
    fun `given new schedule when opening then the reminder tab is selected and both tabs have the tab role`() {
        showScreen()

        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_MODE_REMINDER_TAB)
            .assertIsSelected()
            .assertContentDescriptionEquals(context.getString(R.string.upsert_mode_reminder_description))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_MODE_EVENT_TAB)
            .assertIsNotSelected()
            .assertContentDescriptionEquals(context.getString(R.string.upsert_mode_event_description))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
    }

    @Test
    fun `given reminder mode when selecting the event tab then it is selected and event only rows appear`() {
        showScreen()
        assertEquals(0, tagCount(SemanticsTags.UPSERT_LOCATION_INPUT))
        assertEquals(0, tagCount(SemanticsTags.UPSERT_ALL_DAY_SWITCH))

        click(SemanticsTags.UPSERT_MODE_EVENT_TAB)

        composeRule.onNodeWithTag(SemanticsTags.UPSERT_MODE_EVENT_TAB).assertIsSelected()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_MODE_REMINDER_TAB).assertIsNotSelected()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_LOCATION_INPUT).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_ALL_DAY_SWITCH).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_END_DATE_SELECTOR_ROW).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_CALENDAR_SOURCE_ROW).assertIsDisplayed()
    }

    @Test
    fun `given typed title when switching modes then the title is preserved`() {
        showScreen()
        typeTitle("Consulta")

        click(SemanticsTags.UPSERT_MODE_EVENT_TAB)
        click(SemanticsTags.UPSERT_MODE_REMINDER_TAB)

        composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).assert(hasText("Consulta"))
    }

    // endregion

    // region seletores inline

    @Test
    fun `given collapsed rows when tapping the date row then the calendar opens with expanded state and tapping again closes it`() {
        showScreen()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_DATE_SELECTOR_ROW).assert(stateDescriptionIs(collapsedState()))

        click(SemanticsTags.UPSERT_DATE_SELECTOR_ROW)
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_INLINE_CALENDAR).assertIsDisplayed()
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_INLINE_CALENDAR)
            .assertContentDescriptionEquals(context.getString(R.string.upsert_inline_calendar_description))
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_DATE_SELECTOR_ROW).assert(stateDescriptionIs(expandedState()))

        click(SemanticsTags.UPSERT_DATE_SELECTOR_ROW)
        composeRule.waitUntil(timeoutMillis = 5_000) { tagCount(SemanticsTags.UPSERT_INLINE_CALENDAR) == 0 }
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_DATE_SELECTOR_ROW).assert(stateDescriptionIs(collapsedState()))
    }

    @Test
    fun `given calendar open when tapping the time row then only the time picker stays open`() {
        showScreen()
        click(SemanticsTags.UPSERT_DATE_SELECTOR_ROW)
        assertEquals(1, tagCount(SemanticsTags.UPSERT_INLINE_CALENDAR))

        click(SemanticsTags.UPSERT_TIME_SELECTOR_ROW)

        composeRule.waitUntil(timeoutMillis = 5_000) { tagCount(SemanticsTags.UPSERT_INLINE_CALENDAR) == 0 }
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_INLINE_TIME_PICKER).assertIsDisplayed()
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_INLINE_TIME_PICKER)
            .assertContentDescriptionEquals(context.getString(R.string.upsert_inline_time_picker_description))
        assertEquals(1, tagCount(SemanticsTags.UPSERT_INLINE_TIME_PICKER))
    }

    @Test
    fun `given reminder date picker open when tapping the tomorrow chip then the date row shows tomorrow`() {
        showScreen(initialDate = null)
        click(SemanticsTags.UPSERT_DATE_SELECTOR_ROW)

        click(SemanticsTags.UPSERT_QUICK_CHIP_PREFIX + "tomorrow")

        val tomorrowLabel = LocalDate.now().plusDays(1).toShortDateLabel()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_DATE_SELECTOR_ROW).assert(descriptionContains(tomorrowLabel))
    }

    @Test
    fun `given time picker open when tapping a fixed time chip then the time row shows that time`() {
        showScreen()
        click(SemanticsTags.UPSERT_TIME_SELECTOR_ROW)

        click(SemanticsTags.UPSERT_QUICK_CHIP_PREFIX + "at_15_00")

        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_TIME_SELECTOR_ROW)
            .assert(descriptionContains(LocalTime.of(15, 0).toTimeLabel()))
        assertEquals(1, tagCount(SemanticsTags.UPSERT_QUICK_CHIP_PREFIX + "in_1_hour"))
        assertEquals(1, tagCount(SemanticsTags.UPSERT_QUICK_CHIP_PREFIX + "at_07_00"))
    }

    @Test
    fun `given event mode when turning all day on then the time rows are hidden and the date rows stay`() {
        showScreen()
        click(SemanticsTags.UPSERT_MODE_EVENT_TAB)
        assertEquals(1, tagCount(SemanticsTags.UPSERT_TIME_SELECTOR_ROW))
        assertEquals(1, tagCount(SemanticsTags.UPSERT_END_TIME_SELECTOR_ROW))

        click(SemanticsTags.UPSERT_ALL_DAY_SWITCH)

        assertEquals(0, tagCount(SemanticsTags.UPSERT_TIME_SELECTOR_ROW))
        assertEquals(0, tagCount(SemanticsTags.UPSERT_END_TIME_SELECTOR_ROW))
        assertEquals(1, tagCount(SemanticsTags.UPSERT_DATE_SELECTOR_ROW))
        assertEquals(1, tagCount(SemanticsTags.UPSERT_END_DATE_SELECTOR_ROW))
    }

    // endregion

    @Test
    fun `given new schedule when opening then the recurrence row describes it as not repeating`() {
        showScreen()

        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_RECURRENCE_ROW)
            .assertContentDescriptionEquals(
                context.getString(
                    R.string.upsert_row_description,
                    context.getString(R.string.upsert_recurrence_label),
                    context.getString(R.string.recurrence_none),
                ),
            )
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_RECURRENCE_ROW).assertContentDescriptionEquals("Repetição: Não repetir")
    }

    // region salvar

    @Test
    fun `given blank title when tapping save then it shows the title error and does not leave the screen`() {
        showScreen()

        click(SemanticsTags.UPSERT_SAVE_BUTTON)
        waitUntilWithMainLooper {
            composeRule.onAllNodesWithText(context.getString(R.string.upsert_title_error)).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(context.getString(R.string.upsert_title_error)).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_SAVE_BUTTON).assertIsEnabled()
        assertEquals(0, backCalls)
        assertTrue(runBlocking { repository.getAll() }.isEmpty())
    }

    @Test
    fun `given date picked in a negative offset zone when saving then the stored date equals the chosen date and the alarm is scheduled`() {
        showScreen()
        typeTitle("  Pagar aluguel  ")
        click(SemanticsTags.UPSERT_DATE_SELECTOR_ROW)
        val chosenDay = routeDate.withDayOfMonth(20)

        // Cada dia do M3 DatePicker expõe o texto "Sexta-feira, 20 de janeiro de 2027".
        composeRule
            .onNode(hasText(", 20 de ", substring = true) and hasAnyAncestor(hasTestTag(SemanticsTags.UPSERT_INLINE_CALENDAR)))
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_DATE_SELECTOR_ROW).assert(descriptionContains(chosenDay.toShortDateLabel()))
        click(SemanticsTags.UPSERT_SAVE_BUTTON)
        waitUntilWithMainLooper { backCalls > 0 }

        val stored = runBlocking { repository.getAll() }.single()
        assertEquals(chosenDay, stored.startDate)
        assertEquals("Pagar aluguel", stored.title)
        assertEquals(ScheduleType.REMINDER, stored.type)
        assertEquals(CalendarSource.MY_REMINDERS, stored.calendarSource)
        assertEquals(1, backCalls)
        assertEquals(1, shadowOf(alarmManager).scheduledAlarms.size)
    }

    @Test
    fun `given event filled in when saving then the event is stored with its end location and calendar`() {
        showScreen()
        click(SemanticsTags.UPSERT_MODE_EVENT_TAB)
        typeTitle("Workshop")
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_LOCATION_INPUT).performTextInput("Sala 3")
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_NOTES_INPUT).performTextInput("Levar notebook")

        click(SemanticsTags.UPSERT_SAVE_BUTTON)
        waitUntilWithMainLooper { backCalls > 0 }

        val stored = runBlocking { repository.getAll() }.single()
        assertEquals(ScheduleType.EVENT, stored.type)
        assertEquals("Sala 3", stored.location)
        assertEquals("Levar notebook", stored.notes)
        assertEquals(CalendarSource.PERSONAL_ACCOUNT, stored.calendarSource)
        assertEquals(stored.startTime.plusHours(1), stored.endTime)
    }

    @Test
    fun `given stored schedule when editing and saving then the same item is updated without duplicating`() {
        runBlocking { repository.upsert(scheduleItem(id = "stored-1", title = "Consulta", startDate = routeDate)) }
        showScreen(scheduleId = "stored-1", initialDate = null)
        composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).assert(hasText("Consulta"))

        composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).performTextReplacement("Consulta revisada")
        click(SemanticsTags.UPSERT_SAVE_BUTTON)
        waitUntilWithMainLooper { backCalls > 0 }

        val stored = runBlocking { repository.getAll() }.single()
        assertEquals("stored-1", stored.id)
        assertEquals("Consulta revisada", stored.title)
    }

    @Test
    fun `given unknown schedule id when opening then it leaves the screen`() {
        backCalls = 0
        composeRule.setContent {
            AndroidEventSchedulerTheme {
                UpsertScheduleScreen(scheduleId = "missing", initialDateEpochDay = null, onBack = { backCalls++ })
            }
        }

        waitUntilWithMainLooper { backCalls > 0 }

        assertEquals(1, backCalls)
    }

    @Test
    fun `given cancel button when tapped then it leaves the screen without saving`() {
        showScreen()
        typeTitle("Descartar")

        click(SemanticsTags.UPSERT_CANCEL_BUTTON)
        waitUntilWithMainLooper { backCalls > 0 }

        assertTrue(runBlocking { repository.getAll() }.isEmpty())
    }

    // endregion

    // region cor e adesivo (spec 09)

    @Test
    fun `given new schedule when opening then the sticker and color buttons describe the defaults and have the button role`() {
        showScreen()

        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_STICKER_BUTTON)
            .assertContentDescriptionEquals("Selecionar emoji ou adesivo")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_COLOR_BUTTON)
            .assertContentDescriptionEquals("Cor selecionada: Azul claro. Toque para alterar")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `given the sticker button when choosing an emoji then the dialog closes and the description names it`() {
        showScreen()

        click(SemanticsTags.UPSERT_STICKER_BUTTON)
        composeRule.onNodeWithTag(SemanticsTags.STICKER_PICKER_DIALOG).assertExists()
        click(SemanticsTags.STICKER_OPTION_PREFIX + StickerEmoji.ROCKET.name)

        assertEquals(0, tagCount(SemanticsTags.STICKER_PICKER_DIALOG))
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_STICKER_BUTTON)
            .assertContentDescriptionEquals("Adesivo selecionado: 🚀. Toque para alterar")
    }

    @Test
    fun `given a chosen sticker when removing it then the button goes back to the default description`() {
        showScreen()
        click(SemanticsTags.UPSERT_STICKER_BUTTON)
        click(SemanticsTags.STICKER_OPTION_PREFIX + StickerEmoji.STAR.name)

        click(SemanticsTags.UPSERT_STICKER_BUTTON)
        click(SemanticsTags.STICKER_CLEAR_BUTTON)

        composeRule.onNodeWithTag(SemanticsTags.UPSERT_STICKER_BUTTON).assertContentDescriptionEquals("Selecionar emoji ou adesivo")
    }

    @Test
    fun `given the color button when choosing a color then the dialog closes and the description names it`() {
        showScreen()

        click(SemanticsTags.UPSERT_COLOR_BUTTON)
        composeRule.onNodeWithTag(SemanticsTags.COLOR_PICKER_DIALOG).assertExists()
        click(SemanticsTags.COLOR_OPTION_PREFIX + EventColorOption.CORAL.name)

        assertEquals(0, tagCount(SemanticsTags.COLOR_PICKER_DIALOG))
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_COLOR_BUTTON)
            .assertContentDescriptionEquals("Cor selecionada: Coral. Toque para alterar")
    }

    @Test
    fun `given color and sticker chosen when saving then the stored item carries both`() {
        showScreen()
        typeTitle("Treino de judô")
        click(SemanticsTags.UPSERT_STICKER_BUTTON)
        click(SemanticsTags.STICKER_OPTION_PREFIX + StickerEmoji.WORKOUT.name)
        click(SemanticsTags.UPSERT_COLOR_BUTTON)
        click(SemanticsTags.COLOR_OPTION_PREFIX + EventColorOption.PURPLE.name)

        click(SemanticsTags.UPSERT_SAVE_BUTTON)
        waitUntilWithMainLooper { backCalls > 0 }

        val stored = runBlocking { repository.getAll() }.single()
        assertEquals(EventColorOption.PURPLE.hex, stored.colorHex)
        assertEquals(StickerEmoji.WORKOUT.emoji, stored.stickerEmoji)
    }

    @Test
    fun `given stored item with color and sticker when editing then the buttons come filled and the selection is preselected`() {
        runBlocking {
            repository.upsert(
                scheduleItem(id = "stored-2", title = "Estudo", startDate = routeDate)
                    .copy(colorHex = EventColorOption.INDIGO.hex, stickerEmoji = StickerEmoji.STUDY.emoji),
            )
        }
        showScreen(scheduleId = "stored-2", initialDate = null)

        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_STICKER_BUTTON)
            .assertContentDescriptionEquals("Adesivo selecionado: 📚. Toque para alterar")
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_COLOR_BUTTON)
            .assertContentDescriptionEquals("Cor selecionada: Índigo. Toque para alterar")
        click(SemanticsTags.UPSERT_COLOR_BUTTON)
        composeRule.onNodeWithTag(SemanticsTags.COLOR_OPTION_PREFIX + EventColorOption.INDIGO.name).assertIsSelected()
    }

    @Test
    fun `given stored item with a hex outside the catalog when editing then the button shows the default color`() {
        runBlocking {
            repository.upsert(scheduleItem(id = "stored-3", title = "Antigo", startDate = routeDate).copy(colorHex = 0xFF123456))
        }
        showScreen(scheduleId = "stored-3", initialDate = null)

        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_COLOR_BUTTON)
            .assertContentDescriptionEquals("Cor selecionada: Azul claro. Toque para alterar")
    }

    @Test
    fun `given the color dialog open when dismissing it then the color is unchanged`() {
        showScreen()
        click(SemanticsTags.UPSERT_COLOR_BUTTON)

        composeRule.runOnUiThread { checkNotNull(ShadowDialog.getLatestDialog()).onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(0, tagCount(SemanticsTags.COLOR_PICKER_DIALOG))
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_COLOR_BUTTON)
            .assertContentDescriptionEquals("Cor selecionada: Azul claro. Toque para alterar")
    }

    // endregion

    private fun stateDescriptionIs(expected: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected)

    private fun descriptionContains(text: String) =
        SemanticsMatcher("content description contains '$text'") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.contains(text) } == true
        }
}

package com.br.androideventscheduler.feature.upsert.presentation.component

import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.model.RecurrencePattern
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.testing.pressBackOnLatestDialog
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.upsert.presentation.screen.UpsertScheduleScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Diálogo de repetição dentro da tela de criar/editar, com o grafo real (ViewModel via Koin, Room em memória).
 * Fluxo do PDF adaptado: linha "Repetição" → "Toda semana" → dias → número de vezes → Concluir.
 */
@RunWith(RobolectricTestRunner::class)
// Tela alta: a LazyColumn só compõe os itens visíveis e o formulário é longo.
@Config(qualifiers = "pt-rBR-w411dp-h2400dp")
class RecurrencePickerDialogRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository: ScheduleRepository get() = GlobalContext.get().get()
    private var backCalls = 0

    // Sempre uma quinta-feira futura: o dia da data inicial é o que vem marcado em "Toda semana".
    private val thursdayStart: LocalDate = LocalDate.now().plusDays(14).with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY))

    private fun showScreen(
        scheduleId: String? = null,
        initialDate: LocalDate? = thursdayStart,
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

    private fun dayToggleTag(day: DayOfWeek) = SemanticsTags.RECURRENCE_DAY_TOGGLE_PREFIX + day.name

    private fun openDialog() {
        click(SemanticsTags.UPSERT_RECURRENCE_ROW)
        assertEquals(1, tagCount(SemanticsTags.RECURRENCE_DIALOG))
    }

    private fun assertRecurrenceRow(expectedValue: String) {
        composeRule
            .onNodeWithTag(SemanticsTags.UPSERT_RECURRENCE_ROW)
            .assertContentDescriptionEquals("${context.getString(R.string.upsert_recurrence_label)}: $expectedValue")
    }

    private fun saveAndGetStoredItem() =
        run {
            composeRule.onNodeWithTag(SemanticsTags.UPSERT_TITLE_INPUT).performTextInput("Academia")
            click(SemanticsTags.UPSERT_SAVE_BUTTON)
            waitUntilWithMainLooper { backCalls > 0 }
            runBlocking { repository.getAll() }.single()
        }

    // region abertura

    @Test
    fun `given the form when tapping the recurrence row then the dialog opens with a heading and none selected`() {
        showScreen()
        assertRecurrenceRow("Não repetir")
        assertEquals(0, tagCount(SemanticsTags.RECURRENCE_DIALOG))

        openDialog()

        composeRule
            .onNodeWithTag(SemanticsTags.RECURRENCE_DIALOG)
            .assertContentDescriptionEquals(context.getString(R.string.recurrence_dialog_description))
        composeRule
            .onNode(hasText("Repetição") and hasAnyAncestor(hasTestTag(SemanticsTags.RECURRENCE_DIALOG)))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_OPTION_NONE).assertIsSelected()
        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_OPTION_DAILY).assertIsNotSelected()
        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_OPTION_WEEKLY).assertIsNotSelected()
        assertEquals(0, tagCount(SemanticsTags.RECURRENCE_COUNT_INPUT))
        assertEquals(0, tagCount(dayToggleTag(DayOfWeek.MONDAY)))
    }

    @Test
    fun `given the three options when inspecting semantics then each one is a radio button`() {
        showScreen()
        openDialog()

        listOf(
            SemanticsTags.RECURRENCE_OPTION_NONE,
            SemanticsTags.RECURRENCE_OPTION_DAILY,
            SemanticsTags.RECURRENCE_OPTION_WEEKLY,
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
    }

    @Test
    fun `given a stored weekly item when editing and opening the dialog then it reflects the current pattern`() {
        val pattern = RecurrencePattern.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), occurrenceLimit = 10)
        runBlocking { repository.upsert(scheduleItem(id = "stored-1", startDate = thursdayStart, recurrencePattern = pattern)) }
        showScreen(scheduleId = "stored-1", initialDate = null)
        assertRecurrenceRow("Toda semana em seg., sex. (10 vezes)")

        openDialog()

        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_OPTION_WEEKLY).assertIsSelected()
        composeRule.onNodeWithTag(dayToggleTag(DayOfWeek.MONDAY)).assertIsOn()
        composeRule.onNodeWithTag(dayToggleTag(DayOfWeek.FRIDAY)).assertIsOn()
        composeRule.onNodeWithTag(dayToggleTag(DayOfWeek.THURSDAY)).assertIsOff()
        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_COUNT_INPUT).assert(hasText("10"))
    }

    // endregion

    // region opções e dias

    @Test
    fun `given none selected when choosing weekly then the start date weekday is checked and the day toggles have accessible semantics`() {
        showScreen()
        openDialog()

        click(SemanticsTags.RECURRENCE_OPTION_WEEKLY)

        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_OPTION_WEEKLY).assertIsSelected()
        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_OPTION_NONE).assertIsNotSelected()
        DayOfWeek.entries.forEach { day ->
            val isStartDay = day == DayOfWeek.THURSDAY
            val toggle = composeRule.onNodeWithTag(dayToggleTag(day))
            if (isStartDay) toggle.assertIsOn() else toggle.assertIsOff()
            toggle.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            toggle.assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    context.getString(if (isStartDay) R.string.upsert_state_selected else R.string.upsert_state_not_selected),
                ),
            )
        }
        composeRule.onNodeWithTag(dayToggleTag(DayOfWeek.TUESDAY)).assertContentDescriptionEquals("Terça-feira")
        composeRule.onNodeWithTag(dayToggleTag(DayOfWeek.SUNDAY)).assertContentDescriptionEquals("Domingo")
        assertEquals(1, tagCount(SemanticsTags.RECURRENCE_COUNT_INPUT))
    }

    @Test
    fun `given weekly with a single day when tapping that day then it stays selected`() {
        showScreen()
        openDialog()
        click(SemanticsTags.RECURRENCE_OPTION_WEEKLY)

        click(dayToggleTag(DayOfWeek.THURSDAY))

        composeRule.onNodeWithTag(dayToggleTag(DayOfWeek.THURSDAY)).assertIsOn()
    }

    @Test
    fun `given the count field when typing letters and more than four digits then only four digits are kept`() {
        showScreen()
        openDialog()
        click(SemanticsTags.RECURRENCE_OPTION_DAILY)

        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_COUNT_INPUT).performTextInput("12ab3456")

        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_COUNT_INPUT).assert(hasText("1234"))
        composeRule
            .onNodeWithTag(SemanticsTags.RECURRENCE_COUNT_INPUT)
            .assertContentDescriptionEquals(context.getString(R.string.recurrence_count_description))
    }

    @Test
    fun `given the dialog when choosing none after weekly then the days and the count field disappear`() {
        showScreen()
        openDialog()
        click(SemanticsTags.RECURRENCE_OPTION_WEEKLY)

        click(SemanticsTags.RECURRENCE_OPTION_NONE)

        assertEquals(0, tagCount(SemanticsTags.RECURRENCE_COUNT_INPUT))
        assertEquals(0, tagCount(dayToggleTag(DayOfWeek.THURSDAY)))
    }

    // endregion

    // region confirmar e cancelar

    @Test
    fun `given the weekly flow when adding tuesday typing 5000 and confirming then the row shows the weekly label with the limit`() {
        showScreen()
        openDialog()

        click(SemanticsTags.RECURRENCE_OPTION_WEEKLY)
        click(dayToggleTag(DayOfWeek.TUESDAY))
        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_COUNT_INPUT).performTextInput("5000")
        click(SemanticsTags.RECURRENCE_CONFIRM_BUTTON)

        assertEquals(0, tagCount(SemanticsTags.RECURRENCE_DIALOG))
        assertRecurrenceRow("Toda semana em ter., qui. (5000 vezes)")
    }

    @Test
    fun `given the weekly flow when confirming and saving then the stored item has the weekly pattern`() {
        showScreen()
        openDialog()
        click(SemanticsTags.RECURRENCE_OPTION_WEEKLY)
        click(dayToggleTag(DayOfWeek.TUESDAY))
        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_COUNT_INPUT).performTextInput("5000")
        click(SemanticsTags.RECURRENCE_CONFIRM_BUTTON)

        val stored = saveAndGetStoredItem()

        val expected = RecurrencePattern.Weekly(setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), occurrenceLimit = 5000)
        assertEquals(expected, stored.recurrencePattern)
    }

    @Test
    fun `given the daily flow without a limit when confirming and saving then the stored pattern is Daily without limit`() {
        showScreen()
        openDialog()

        click(SemanticsTags.RECURRENCE_OPTION_DAILY)
        click(SemanticsTags.RECURRENCE_CONFIRM_BUTTON)

        assertRecurrenceRow("Diariamente")
        assertEquals(RecurrencePattern.Daily(occurrenceLimit = null), saveAndGetStoredItem().recurrencePattern)
    }

    @Test
    fun `given the dialog with changes when cancelling then the pattern is unchanged and reopening starts from the current pattern`() {
        showScreen()
        openDialog()
        click(SemanticsTags.RECURRENCE_OPTION_WEEKLY)
        click(dayToggleTag(DayOfWeek.TUESDAY))

        click(SemanticsTags.RECURRENCE_DISMISS_BUTTON)

        assertEquals(0, tagCount(SemanticsTags.RECURRENCE_DIALOG))
        assertRecurrenceRow("Não repetir")
        openDialog()
        composeRule.onNodeWithTag(SemanticsTags.RECURRENCE_OPTION_NONE).assertIsSelected()
    }

    @Test
    fun `given the dialog with changes when pressing back then it closes without changing the pattern`() {
        showScreen()
        openDialog()
        click(SemanticsTags.RECURRENCE_OPTION_DAILY)

        // O diálogo do Compose é um ComponentDialog: o botão voltar chega ao dispatcher dele, não ao da Activity.
        composeRule.runOnUiThread { pressBackOnLatestDialog() }
        composeRule.waitForIdle()

        assertEquals(0, tagCount(SemanticsTags.RECURRENCE_DIALOG))
        assertRecurrenceRow("Não repetir")
        assertTrue(backCalls == 0)
    }

    // endregion
}

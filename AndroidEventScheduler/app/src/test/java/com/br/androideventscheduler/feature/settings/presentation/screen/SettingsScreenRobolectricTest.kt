package com.br.androideventscheduler.feature.settings.presentation.screen

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.preferences.domain.ThemeMode
import com.br.androideventscheduler.core.preferences.domain.UserSettings
import com.br.androideventscheduler.core.schedule.domain.model.ReminderOffset
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.settings.presentation.event.SettingsUiEvent
import com.br.androideventscheduler.feature.settings.presentation.state.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class SettingsScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<SettingsUiEvent>()

    private fun setContent(uiState: SettingsUiState) {
        composeRule.setContent {
            AndroidEventSchedulerTheme { SettingsContent(uiState = uiState, onEvent = { events += it }) }
        }
    }

    @Test
    fun `given loading state when rendered then progress is shown`() {
        setContent(SettingsUiState.Loading)

        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_LOADING).assertIsDisplayed()
    }

    @Test
    fun `given content when typing email then change event carries the text`() {
        setContent(SettingsUiState.Content(draft = UserSettings()))

        composeRule
            .onNodeWithTag(SemanticsTags.SETTINGS_ACCOUNT_EMAIL_INPUT)
            .assertContentDescriptionContains(context.getString(R.string.settings_account_email_description))
            .performTextReplacement("bia@exemplo.com")

        assertEquals(SettingsUiEvent.OnEmailChanged("bia@exemplo.com"), events.last())
    }

    @Test
    fun `given email error when rendered then supporting error text is shown`() {
        setContent(SettingsUiState.Content(draft = UserSettings(activeAccountEmail = "x"), emailError = true))

        composeRule.onNodeWithText(context.getString(R.string.settings_account_email_error)).assertIsDisplayed()
    }

    @Test
    fun `given reminder options when rendered then they are radio buttons reflecting the selection`() {
        setContent(SettingsUiState.Content(draft = UserSettings(defaultReminderOffset = ReminderOffset.THIRTY_MINUTES)))

        composeRule
            .onNodeWithTag(SemanticsTags.SETTINGS_REMINDER_OPTION_PREFIX + ReminderOffset.THIRTY_MINUTES.name)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
        composeRule
            .onNodeWithTag(SemanticsTags.SETTINGS_REMINDER_OPTION_PREFIX + ReminderOffset.ONE_HOUR.name)
            .assertIsNotSelected()
            .performScrollTo()
            .performClick()

        assertEquals(SettingsUiEvent.OnReminderSelected(ReminderOffset.ONE_HOUR), events.last())
    }

    @Test
    fun `given reminder options when rendered then five minutes sits between no alert and ten minutes and can be chosen`() {
        setContent(SettingsUiState.Content(draft = UserSettings()))

        val fiveMinutesTag = SemanticsTags.SETTINGS_REMINDER_OPTION_PREFIX + ReminderOffset.FIVE_MINUTES.name
        composeRule
            .onNodeWithTag(fiveMinutesTag)
            .assert(hasText(context.resources.getQuantityString(R.plurals.reminder_minutes_before, 5, 5)))
        val optionTops =
            listOf(ReminderOffset.NONE, ReminderOffset.FIVE_MINUTES, ReminderOffset.TEN_MINUTES).map { offset ->
                composeRule
                    .onNodeWithTag(SemanticsTags.SETTINGS_REMINDER_OPTION_PREFIX + offset.name)
                    .fetchSemanticsNode()
                    .positionInRoot.y
            }
        assertTrue("options out of order: $optionTops", optionTops[0] < optionTops[1] && optionTops[1] < optionTops[2])

        composeRule
            .onNodeWithTag(fiveMinutesTag)
            .assertIsNotSelected()
            .performScrollTo()
            .performClick()

        assertEquals(SettingsUiEvent.OnReminderSelected(ReminderOffset.FIVE_MINUTES), events.last())
    }

    @Test
    fun `given snooze options when selecting fifteen then the event carries fifteen`() {
        setContent(SettingsUiState.Content(draft = UserSettings()))

        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_SNOOZE_OPTION_PREFIX + 10).performScrollTo().assertIsSelected()
        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_SNOOZE_OPTION_PREFIX + 15).performScrollTo().performClick()

        assertEquals(SettingsUiEvent.OnSnoozeSelected(15), events.last())
    }

    @Test
    fun `given full screen switch when toggled then it is a switch and emits the new value`() {
        setContent(SettingsUiState.Content(draft = UserSettings(fullScreenEventAlertEnabled = false)))

        composeRule
            .onNodeWithTag(SemanticsTags.SETTINGS_FULLSCREEN_SWITCH)
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff()
            .performClick()

        assertEquals(SettingsUiEvent.OnFullScreenToggled(true), events.last())
    }

    @Test
    fun `given save and back buttons when clicked then their events are emitted`() {
        setContent(SettingsUiState.Content(draft = UserSettings()))

        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_SAVE_BUTTON).performScrollTo().performClick()
        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_BACK_BUTTON).performClick()

        assertTrue(events.containsAll(listOf(SettingsUiEvent.OnSaveClicked, SettingsUiEvent.OnBackClicked)))
    }

    @Test
    fun `given saving state when rendered then save button is disabled`() {
        setContent(SettingsUiState.Content(draft = UserSettings(), isSaving = true))

        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_SAVE_BUTTON).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `given content when rendered then the backup item is a button that emits the backup event`() {
        setContent(SettingsUiState.Content(draft = UserSettings()))

        composeRule
            .onNodeWithTag(SemanticsTags.SETTINGS_BACKUP_ITEM)
            .performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsDisplayed()
            .performClick()

        assertEquals(SettingsUiEvent.OnBackupClicked, events.last())
        composeRule.onNodeWithText(context.getString(R.string.settings_backup_item)).assertIsDisplayed()
    }

    @Test
    fun `given content when rendered then the appearance section lists three radio options with the current one selected`() {
        setContent(SettingsUiState.Content(draft = UserSettings(), themeMode = ThemeMode.SYSTEM))

        composeRule.onNodeWithText(context.getString(R.string.settings_appearance_section)).assertIsDisplayed()
        ThemeMode.entries.forEach { mode ->
            composeRule
                .onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + mode.name)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        }
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_system)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_light)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.settings_theme_dark)).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + ThemeMode.SYSTEM.name).assertIsSelected()
        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + ThemeMode.LIGHT.name).assertIsNotSelected()
        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + ThemeMode.DARK.name).assertIsNotSelected()
    }

    @Test
    fun `given the default state when rendered then dark is the selected theme`() {
        setContent(SettingsUiState.Content(draft = UserSettings()))

        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + ThemeMode.DARK.name).assertIsSelected()
    }

    @Test
    fun `given theme options when clicking light then the appearance event carries light`() {
        setContent(SettingsUiState.Content(draft = UserSettings()))

        composeRule.onNodeWithTag(SemanticsTags.SETTINGS_THEME_OPTION_PREFIX + ThemeMode.LIGHT.name).performScrollTo().performClick()

        assertEquals(SettingsUiEvent.AppearanceEvent.OnThemeModeSelected(ThemeMode.LIGHT), events.last())
    }
}

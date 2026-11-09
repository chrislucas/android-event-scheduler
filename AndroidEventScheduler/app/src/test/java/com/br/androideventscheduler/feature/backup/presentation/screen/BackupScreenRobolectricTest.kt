package com.br.androideventscheduler.feature.backup.presentation.screen

import android.content.Context
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.backup.domain.model.ImportStrategy
import com.br.androideventscheduler.feature.backup.presentation.component.previewImport
import com.br.androideventscheduler.feature.backup.presentation.event.BackupUiEvent
import com.br.androideventscheduler.feature.backup.presentation.state.BackupUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class BackupScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<BackupUiEvent>()

    private fun setContent(uiState: BackupUiState) {
        composeRule.setContent {
            AndroidEventSchedulerTheme { BackupContent(uiState = uiState, onEvent = { events += it }) }
        }
    }

    private fun isButton() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun isRadio() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun `given loading state when rendered then progress is shown`() {
        setContent(BackupUiState.Loading)

        composeRule.onNodeWithTag(SemanticsTags.BACKUP_LOADING).assertIsDisplayed()
    }

    @Test
    fun `given content when rendered then export and import sections with their explanations are shown`() {
        setContent(BackupUiState.Content(itemCount = 3))

        composeRule.onNodeWithText(context.getString(R.string.backup_export_section)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.backup_export_description)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.backup_import_section)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.backup_import_description)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `given content when rendered then the encryption warning is shown`() {
        setContent(BackupUiState.Content(itemCount = 3))

        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_ENCRYPTION_WARNING)
            .performScrollTo()
            .assertTextEquals(context.getString(R.string.backup_encryption_warning))
    }

    @Test
    fun `given items when clicking share and save then the matching events are sent and both are buttons`() {
        setContent(BackupUiState.Content(itemCount = 3))

        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_EXPORT_SHARE_BUTTON)
            .assert(isButton())
            .assertIsEnabled()
            .performClick()
        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_EXPORT_SAVE_BUTTON)
            .assert(isButton())
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf(BackupUiEvent.OnShareExportClicked, BackupUiEvent.OnSaveExportClicked), events)
    }

    @Test
    fun `given empty agenda when rendered then both export buttons are disabled with the explanation`() {
        setContent(BackupUiState.Content(itemCount = 0))

        composeRule.onNodeWithTag(SemanticsTags.BACKUP_EXPORT_SHARE_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_EXPORT_SAVE_BUTTON).assertIsNotEnabled()
        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_EXPORT_EMPTY_MESSAGE)
            .assertTextEquals(context.getString(R.string.backup_export_empty))
    }

    @Test
    fun `given an export in progress when rendered then progress is shown and the buttons are disabled`() {
        setContent(BackupUiState.Content(itemCount = 3, isExporting = true))

        composeRule.onNodeWithTag(SemanticsTags.BACKUP_EXPORT_PROGRESS).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_EXPORT_SHARE_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_EXPORT_SAVE_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PICK_BUTTON).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `given content when clicking pick file then the pick event is sent`() {
        setContent(BackupUiState.Content(itemCount = 3))

        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PICK_BUTTON)
            .performScrollTo()
            .assert(isButton())
            .performClick()

        assertEquals(listOf<BackupUiEvent>(BackupUiEvent.OnPickImportClicked), events)
    }

    @Test
    fun `given a refused file when rendered then the reason is shown and there is no preview`() {
        setContent(BackupUiState.Content(itemCount = 3, importErrorRes = R.string.backup_error_not_backup))

        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_ERROR)
            .performScrollTo()
            .assertTextEquals(context.getString(R.string.backup_error_not_backup))
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW).assertDoesNotExist()
    }

    @Test
    fun `given a preview when rendered then counts use plurals for new existing and invalid items`() {
        setContent(BackupUiState.Content(itemCount = 3, preview = previewImport(newCount = 1, existingCount = 2, invalidCount = 1)))

        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_TOTAL).performScrollTo().assertTextEquals("4 agendamentos no arquivo")
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_NEW).performScrollTo().assertTextEquals("1 novo")
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_EXISTING).performScrollTo().assertTextEquals("2 já existem na agenda")
        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_INVALID)
            .performScrollTo()
            .assertTextEquals("1 item ignorado por dados inválidos")
    }

    @Test
    fun `given a preview without invalid items when rendered then the ignored line is absent`() {
        setContent(BackupUiState.Content(itemCount = 3, preview = previewImport(invalidCount = 0)))

        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PREVIEW_INVALID).assertDoesNotExist()
    }

    @Test
    fun `given existing items when rendered then the strategies are radio buttons reflecting the selection`() {
        setContent(BackupUiState.Content(itemCount = 3, preview = previewImport(), strategy = ImportStrategy.KEEP_EXISTING))

        val prefix = SemanticsTags.BACKUP_IMPORT_STRATEGY_OPTION_PREFIX
        composeRule
            .onNodeWithTag(prefix + ImportStrategy.REPLACE_EXISTING.name)
            .performScrollTo()
            .assert(isRadio())
            .assertIsNotSelected()
        composeRule
            .onNodeWithTag(prefix + ImportStrategy.KEEP_EXISTING.name)
            .performScrollTo()
            .assert(isRadio())
            .assertIsSelected()
        composeRule
            .onNodeWithTag(prefix + ImportStrategy.IMPORT_AS_COPIES.name)
            .performScrollTo()
            .assert(isRadio())
            .assertIsNotSelected()
    }

    @Test
    fun `given the strategies when selecting one then the selection event is sent`() {
        setContent(BackupUiState.Content(itemCount = 3, preview = previewImport()))

        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_STRATEGY_OPTION_PREFIX + ImportStrategy.IMPORT_AS_COPIES.name)
            .performScrollTo()
            .performClick()

        assertEquals(listOf<BackupUiEvent>(BackupUiEvent.OnStrategySelected(ImportStrategy.IMPORT_AS_COPIES)), events)
    }

    @Test
    fun `given only new items when rendered then the strategy choice is not offered`() {
        setContent(BackupUiState.Content(itemCount = 3, preview = previewImport(newCount = 2, existingCount = 0, invalidCount = 0)))

        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_STRATEGY_GROUP).assertDoesNotExist()
    }

    @Test
    fun `given a preview when confirming and cancelling then the matching events are sent`() {
        setContent(BackupUiState.Content(itemCount = 3, preview = previewImport()))

        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CONFIRM_BUTTON)
            .performScrollTo()
            .assert(isButton())
            .performClick()
        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CANCEL_BUTTON)
            .performScrollTo()
            .assert(isButton())
            .performClick()

        assertEquals(listOf(BackupUiEvent.OnConfirmImportClicked, BackupUiEvent.OnCancelImportClicked), events)
    }

    @Test
    fun `given an import in progress when rendered then confirm cancel and strategies are disabled`() {
        setContent(BackupUiState.Content(itemCount = 3, preview = previewImport(), isImporting = true))

        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CONFIRM_BUTTON).performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag(SemanticsTags.BACKUP_IMPORT_CANCEL_BUTTON).performScrollTo().assertIsNotEnabled()
        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_STRATEGY_OPTION_PREFIX + ImportStrategy.KEEP_EXISTING.name)
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun `given a file being read when rendered then the reading progress is announced`() {
        setContent(BackupUiState.Content(itemCount = 3, isReadingImport = true))

        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_IMPORT_PROGRESS)
            .performScrollTo()
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun `given an export in progress when rendered then the progress is a live region`() {
        setContent(BackupUiState.Content(itemCount = 3, isExporting = true))

        composeRule
            .onNodeWithTag(SemanticsTags.BACKUP_EXPORT_PROGRESS)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun `given the back button when clicked then the back event is sent`() {
        setContent(BackupUiState.Content(itemCount = 3))

        composeRule.onNodeWithTag(SemanticsTags.BACKUP_BACK_BUTTON).performClick()

        assertEquals(listOf<BackupUiEvent>(BackupUiEvent.OnBackClicked), events)
    }
}

package com.br.androideventscheduler.feature.auth.presentation.screen

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.auth.presentation.event.LockUiEvent
import com.br.androideventscheduler.feature.auth.presentation.state.LockUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class LockScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val events = mutableListOf<LockUiEvent>()

    private fun setContent(uiState: LockUiState) {
        composeRule.setContent {
            AndroidEventSchedulerTheme { LockContent(uiState = uiState, onEvent = { events += it }) }
        }
    }

    @Test
    fun `given idle state when rendered then title message and unlock button come from resources`() {
        setContent(LockUiState.Idle)

        composeRule.onNodeWithTag(SemanticsTags.LOCK_SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.lock_title)).assertIsDisplayed()
        composeRule.onNodeWithText("Confirme o bloqueio de tela do celular para continuar").assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.LOCK_UNLOCK_BUTTON).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `given idle state when rendered then the unlock button has the button role`() {
        setContent(LockUiState.Idle)

        composeRule
            .onNodeWithTag(SemanticsTags.LOCK_UNLOCK_BUTTON)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `given idle state when clicking unlock then the unlock event is sent`() {
        setContent(LockUiState.Idle)

        composeRule.onNodeWithTag(SemanticsTags.LOCK_UNLOCK_BUTTON).performClick()

        assertEquals(listOf<LockUiEvent>(LockUiEvent.OnUnlockClicked), events)
    }

    @Test
    fun `given locked out message when rendered then the text is shown and the button stays enabled`() {
        setContent(LockUiState.Message(R.string.lock_locked_out))

        composeRule.onNodeWithTag(SemanticsTags.LOCK_STATUS_MESSAGE).assertTextEquals("Muitas tentativas. Tente novamente mais tarde.")
        composeRule.onNodeWithTag(SemanticsTags.LOCK_UNLOCK_BUTTON).assertIsEnabled()
    }

    @Test
    fun `given not available message when rendered then its resource text is shown`() {
        setContent(LockUiState.Message(R.string.lock_not_available))

        composeRule.onNodeWithTag(SemanticsTags.LOCK_STATUS_MESSAGE).assertTextEquals(context.getString(R.string.lock_not_available))
    }

    @Test
    fun `given idle state when rendered then no status message is shown`() {
        setContent(LockUiState.Idle)

        composeRule.onNodeWithTag(SemanticsTags.LOCK_STATUS_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun `given authenticating state when rendered then progress is shown and the button is disabled`() {
        setContent(LockUiState.Authenticating)

        composeRule.onNodeWithTag(SemanticsTags.LOCK_PROGRESS).assertIsDisplayed()
        composeRule.onNodeWithTag(SemanticsTags.LOCK_UNLOCK_BUTTON).assertIsNotEnabled()
    }
}

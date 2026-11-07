package com.br.androideventscheduler.feature.splash.presentation.screen

import android.content.Context
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SplashScreenRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `given splash content when rendered then logo is an image with accessible description`() {
        composeRule.setContent { AndroidEventSchedulerTheme { SplashContent() } }

        composeRule
            .onNodeWithTag(SemanticsTags.SPLASH_LOGO)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
            .assertContentDescriptionEquals(context.getString(R.string.splash_logo_description))
    }

    @Test
    fun `given splash content when rendered then title is a heading with the brand name`() {
        composeRule.setContent { AndroidEventSchedulerTheme { SplashContent() } }

        composeRule
            .onNodeWithTag(SemanticsTags.SPLASH_TITLE)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertTextEquals(context.getString(R.string.splash_title))
    }
}

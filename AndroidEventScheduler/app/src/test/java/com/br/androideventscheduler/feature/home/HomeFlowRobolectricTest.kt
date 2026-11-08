package com.br.androideventscheduler.feature.home

import android.Manifest
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.schedule.domain.repository.ScheduleRepository
import com.br.androideventscheduler.core.schedule.testing.scheduleItem
import com.br.androideventscheduler.core.ui.SemanticsTags
import com.br.androideventscheduler.feature.home.presentation.screen.HomeScreen
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/**
 * Ponta a ponta com o grafo real: tela → ViewModel (Koin, dispatcher main) → repositório → Room.
 * O resultado do Room retoma na main via `Handler.post`: o looper é drenado dentro do `waitUntil`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR-w411dp-h891dp")
class HomeFlowRobolectricTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val requestedPermissions = mutableListOf<String>()

    private val permissionOwner =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry =
                object : ActivityResultRegistry() {
                    override fun <I, O> onLaunch(
                        requestCode: Int,
                        contract: ActivityResultContract<I, O>,
                        input: I,
                        options: ActivityOptionsCompat?,
                    ) {
                        requestedPermissions += input as String
                        dispatchResult(requestCode, false)
                    }
                }
        }

    private fun waitUntilWithMainLooper(condition: () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
            condition()
        }
    }

    private fun hasNode(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `given a saved item today when the home opens then it is listed and clicking opens its detail`() {
        val repository = GlobalContext.get().get<ScheduleRepository>()
        runBlocking { repository.upsert(scheduleItem(id = "flow-1", title = "Consulta", startDate = LocalDate.now())) }
        var openedDetailId: String? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides permissionOwner) {
                AndroidEventSchedulerTheme {
                    HomeScreen(onNavigateToSearch = {}, onNavigateToUpsert = {}, onNavigateToDetail = { openedDetailId = it })
                }
            }
        }

        waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "flow-1") }
        composeRule.onNodeWithTag(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "flow-1").performClick()
        waitUntilWithMainLooper { openedDetailId != null }

        assertEquals("flow-1", openedDetailId)
        assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), requestedPermissions)
    }

    @Test
    fun `given the open home when an item is saved elsewhere then the list updates without user action`() {
        val repository = GlobalContext.get().get<ScheduleRepository>()
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides permissionOwner) {
                AndroidEventSchedulerTheme {
                    HomeScreen(onNavigateToSearch = {}, onNavigateToUpsert = {}, onNavigateToDetail = {})
                }
            }
        }
        waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_EMPTY_DAY) }

        runBlocking { repository.upsert(scheduleItem(id = "late", title = "Chegou depois", startDate = LocalDate.now())) }
        waitUntilWithMainLooper { hasNode(SemanticsTags.HOME_SCHEDULE_ITEM_PREFIX + "late") }

        assertEquals(false, hasNode(SemanticsTags.HOME_EMPTY_DAY))
    }
}

package com.aistudio.tdbassistant

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.tdbassistant.data.ai.GeminiDiagnostics
import com.aistudio.tdbassistant.data.local.GoalDomain
import com.aistudio.tdbassistant.data.local.MemoryCategory
import com.aistudio.tdbassistant.data.local.TdbDatabase
import com.aistudio.tdbassistant.data.local.TdbLocalRepository
import com.aistudio.tdbassistant.data.preferences.MotionQuality
import com.aistudio.tdbassistant.domain.OrbState
import com.aistudio.tdbassistant.ui.components.AiOrbVisualizer
import com.aistudio.tdbassistant.ui.screens.MemoriesScreen
import com.aistudio.tdbassistant.ui.theme.TdbTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TdbRobolectricAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: TdbDatabase
    private lateinit var repo: TdbLocalRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TdbDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TdbLocalRepository(
            db.chatMessageDao(),
            db.memoryDao(),
            db.dailyGoalDao(),
            db.activityLogDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app resources declare TDB Personal AI name and accessibility description`() {
        assertEquals("TDB Personal AI Assistant", context.getString(R.string.app_name))
        val accDesc = context.getString(R.string.accessibility_service_description)
        assertTrue(accDesc.contains("TDB"))
    }

    @Test
    fun `gemini diagnostics maps HTTP failures with Android JSONObject`() {
        val quota = GeminiDiagnostics.mapHttpFailure(429, "{}")
        assertFalse(quota.ok)
        assertEquals("QUOTA_EXCEEDED", quota.code)

        val invalid = GeminiDiagnostics.mapHttpFailure(
            400,
            """{"error":{"message":"API key not valid","details":[{"reason":"API_KEY_INVALID"}]}}"""
        )
        assertEquals("API_KEY_INVALID", invalid.code)
        assertNotNull(invalid.message)

        val leaked = GeminiDiagnostics.mapHttpFailure(
            403,
            """{"error":{"message":"Your API key was reported as leaked"}}"""
        )
        assertEquals("API_KEY_LEAKED", leaked.code)
    }

    @Test
    fun `local repository supports memory edit delete export and productivity scoring`() {
        runBlocking {
            repo.addMemory(MemoryCategory.GOAL, "Complete v3.0 release")
            val initial = repo.memories.first()
            assertEquals(1, initial.size)

            repo.editMemory(initial.first().id, MemoryCategory.PROJECT, "Shipped TDB v3.0")
            val edited = repo.memories.first()
            assertEquals("Shipped TDB v3.0", edited.first().text)
            assertEquals(MemoryCategory.PROJECT.id, edited.first().category)

            val export = repo.formatExportText(edited)
            assertTrue(export.contains("Shipped TDB v3.0"))

            repo.addGoal("Deep Focus", GoalDomain.STUDY, 60)
            val goals = repo.goals.first()
            assertEquals(1, goals.size)
            repo.logGoalMinutes(goals.first(), 60)
            repo.addActivityLog("Deep Focus", GoalDomain.STUDY, 120)

            val updatedGoals = repo.goals.first()
            val logs = repo.activityLogs.first()
            val score = repo.computeProductivityScore(updatedGoals, logs)
            assertTrue(score > 50)

            repo.deleteMemory(edited.first().id)
            assertTrue(repo.memories.first().isEmpty())
        }
    }

    @Test
    fun `ai orb visualizer renders state label and responds to tap`() {
        var clicked = false
        composeTestRule.setContent {
            TdbTheme {
                AiOrbVisualizer(
                    orbState = OrbState.LISTENING,
                    motionQuality = MotionQuality.BALANCED,
                    wakeWordActive = true,
                    onOrbClick = { clicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_orb_stage").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEY TDB").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_orb_button").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `memories screen filters cards by search input`() {
        val saved = runBlocking {
            repo.addMemory(MemoryCategory.NOTE, "Deep Barmon founded TDB")
            repo.addMemory(MemoryCategory.PREFERENCE, "Likes deep emerald glass UI")
            repo.memories.first()
        }

        composeTestRule.setContent {
            TdbTheme {
                MemoriesScreen(
                    memories = saved,
                    onAddMemory = { _, _ -> },
                    onEditMemory = { _, _, _ -> },
                    onDeleteMemory = {},
                    onExportMemories = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Deep Barmon founded TDB").assertIsDisplayed()
        composeTestRule.onNodeWithTag("memory_search_input").performTextInput("emerald")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Likes deep emerald glass UI").assertIsDisplayed()
    }
}

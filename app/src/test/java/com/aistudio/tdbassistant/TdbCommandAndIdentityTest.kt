package com.aistudio.tdbassistant

import com.aistudio.tdbassistant.data.ai.GeminiDiagnostics
import com.aistudio.tdbassistant.data.local.DailyGoalEntity
import com.aistudio.tdbassistant.data.local.GoalDomain
import com.aistudio.tdbassistant.data.local.MemoryCategory
import com.aistudio.tdbassistant.data.local.MemoryEntity
import com.aistudio.tdbassistant.data.preferences.AiResponseStyle
import com.aistudio.tdbassistant.data.preferences.TdbLanguage
import com.aistudio.tdbassistant.domain.CompanionPose
import com.aistudio.tdbassistant.domain.MessagingChannel
import com.aistudio.tdbassistant.domain.MobileCommandParser
import com.aistudio.tdbassistant.domain.ParsedCommand
import com.aistudio.tdbassistant.domain.PhoneSystemAction
import com.aistudio.tdbassistant.domain.TdbPersonality
import com.aistudio.tdbassistant.domain.WakeWordMatcher
import com.aistudio.tdbassistant.domain.YouTubeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TdbCommandAndIdentityTest {

    @Test
    fun `wake word matcher recognizes bangla and english wake phrases`() {
        assertEquals(true to "", WakeWordMatcher.extractAfterWakeWord("Hey TDB"))
        assertEquals(true to "", WakeWordMatcher.extractAfterWakeWord("Hello TDB!"))
        assertEquals(true to "", WakeWordMatcher.extractAfterWakeWord("হ্যালো টিডিবি"))
        assertEquals(true to "", WakeWordMatcher.extractAfterWakeWord("এই TDB"))
        assertEquals(true to "Open YouTube", WakeWordMatcher.extractAfterWakeWord("Hey TDB, Open YouTube"))
        assertEquals(false to "আজকের আবহাওয়া কেমন?", WakeWordMatcher.extractAfterWakeWord("আজকের আবহাওয়া কেমন?"))
    }

    @Test
    fun `parser handles wake acknowledgement and vision command`() {
        assertEquals(ParsedCommand.WakeAcknowledge, MobileCommandParser.parse("Hey TDB"))
        assertEquals(ParsedCommand.OpenVision, MobileCommandParser.parse("সামনে কী আছে?"))
        assertEquals(ParsedCommand.OpenVision, MobileCommandParser.parse("Open Vision"))
    }

    @Test
    fun `parser handles phone controls and youtube actions`() {
        assertEquals(
            ParsedCommand.PhoneControl(PhoneSystemAction.OPEN_WIFI_SETTINGS),
            MobileCommandParser.parse("Wi-Fi সেটিংস খোলো")
        )
        assertEquals(
            ParsedCommand.PhoneControl(PhoneSystemAction.VOLUME_UP),
            MobileCommandParser.parse("Volume বাড়াও")
        )
        assertEquals(
            ParsedCommand.PhoneControl(PhoneSystemAction.NAVIGATE_BACK),
            MobileCommandParser.parse("আগের স্ক্রিনে ফিরে যাও")
        )
        assertEquals(
            ParsedCommand.YouTubeControl(YouTubeAction.PAUSE),
            MobileCommandParser.parse("ভিডিও pause করো")
        )
        assertEquals(
            ParsedCommand.YouTubeControl(YouTubeAction.NEXT),
            MobileCommandParser.parse("পরবর্তী ভিডিও")
        )
        assertEquals(
            ParsedCommand.YouTubeControl(YouTubeAction.SPEED_2X),
            MobileCommandParser.parse("2x speed দাও")
        )
        assertEquals(
            ParsedCommand.SearchYouTube("Deep Barmon Bhai Vlog"),
            MobileCommandParser.parse("Search Deep Barmon Bhai Vlog on YouTube")
        )
    }

    @Test
    fun `parser handles whatsapp sms call and companion gestures`() {
        val wa = MobileCommandParser.parse("WhatsApp message to Rahat: আসসালামু আলাইকুম")
        assertEquals(
            ParsedCommand.StartMessagingFlow(MessagingChannel.WHATSAPP, "Rahat", "আসসালামু আলাইকুম"),
            wa
        )
        val sms = MobileCommandParser.parse("SMS to +8801700000000: Hello")
        assertEquals(
            ParsedCommand.StartMessagingFlow(MessagingChannel.SMS, "+8801700000000", "Hello"),
            sms
        )
        val call = MobileCommandParser.parse("Call Ammu")
        assertEquals(ParsedCommand.StartCallFlow("Ammu"), call)

        val gesture = MobileCommandParser.parse("হাত নাড়ো")
        assertTrue(gesture is ParsedCommand.CompanionGesture && gesture.gesture == CompanionPose.WAVING)
    }

    @Test
    fun `personality preserves Deep Barmon TDB and Deep Barmon Bhai Vlog identity`() {
        val bn = TdbPersonality.creatorAnswerBn()
        val en = TdbPersonality.creatorAnswerEn()
        assertTrue(bn.contains("Deep Barmon"))
        assertTrue(bn.contains("TDB"))
        assertFalse(bn.contains("TDP"))
        assertTrue(en.contains("Deep Barmon"))
        assertTrue(en.contains("TDB Personal AI Assistant"))
        assertFalse(en.contains("TDP"))

        val instruction = TdbPersonality.buildSystemInstruction(
            language = TdbLanguage.BANGLA,
            responseStyle = AiResponseStyle.BALANCED,
            memories = listOf(MemoryEntity(category = MemoryCategory.GOAL.id, text = "Learn Android daily")),
            goals = listOf(DailyGoalEntity(title = "Study Kotlin", domain = GoalDomain.STUDY.id, targetMinutes = 45))
        )
        assertTrue(instruction.contains("Deep Barmon Bhai Vlog"))
        assertTrue(instruction.contains("Learn Android daily"))
        assertTrue(instruction.contains("Study Kotlin"))
    }

    @Test
    fun `gemini diagnostics validates key and model format cleanly`() {
        assertEquals("KEY_MISSING", GeminiDiagnostics.validateKeyFormat("  ")?.code)
        assertEquals("KEY_FORMAT_INVALID", GeminiDiagnostics.validateKeyFormat("short")?.code)
        assertNull(GeminiDiagnostics.validateKeyFormat("AIzaSyDummyValidLookingKey1234567890"))
        assertEquals("MODEL_FORMAT_INVALID", GeminiDiagnostics.validateModelFormat("bad model!")?.code)
        assertNull(GeminiDiagnostics.validateModelFormat("gemini-3.8-flash"))
    }
}

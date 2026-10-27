package com.swasthai.app

import com.swasthai.app.domain.safety.EmergencyTriage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyTriageTest {

    @Test
    fun testDetectsEmergencyKeywords() {
        val result1 = EmergencyTriage.evaluate("I have severe crushing chest pain since 20 minutes")
        assertTrue("Should detect emergency", result1.isEmergency)
        assertNotNull(result1.emergencyNotice)
        assertTrue(result1.emergencyNotice!!.contains("112"))

        val result2 = EmergencyTriage.evaluate("My father cannot breathe properly and feels dizzy")
        assertTrue(result2.isEmergency)

        val result3 = EmergencyTriage.evaluate("Experiencing sudden facial droop and slurred speech")
        assertTrue(result3.isEmergency)
    }

    @Test
    fun testAllowsNonEmergencyMedicalQueries() {
        val result1 = EmergencyTriage.evaluate("What is the reference range for Fasting Blood Sugar?")
        assertFalse(result1.isEmergency)

        val result2 = EmergencyTriage.evaluate("Explain my latest CBC report from Dr. Mehta")
        assertFalse(result2.isEmergency)

        val result3 = EmergencyTriage.evaluate("Can you summarize the difference between my 2025 and 2026 reports?")
        assertFalse(result3.isEmergency)
    }

    @Test
    fun testDisclaimerTextIntegrity() {
        assertTrue(EmergencyTriage.MEDICAL_DISCLAIMER.contains("health education"))
        assertTrue(EmergencyTriage.MEDICAL_DISCLAIMER.contains("licensed healthcare professional"))
    }
}

package com.swasthai.app.domain.safety

data class TriageResult(
    val isEmergency: Boolean,
    val triggerPattern: String? = null,
    val emergencyNotice: String? = null
)

object EmergencyTriage {

    private val EMERGENCY_KEYWORDS = listOf(
        "chest pain",
        "crushing chest",
        "heart attack",
        "cannot breathe",
        "shortness of breath",
        "difficulty breathing",
        "severe bleeding",
        "unconscious",
        "fainted",
        "stroke",
        "facial droop",
        "slurred speech",
        "sudden numbness",
        "coughing up blood",
        "anaphylaxis",
        "severe allergic reaction",
        "overdose",
        "suicide",
        "killing myself",
        "सीने में दर्द",
        "छाती में दर्द",
        "सांस नहीं आ रही",
        "सांस लेने में तकलीफ",
        "बहुत खून बह",
        "बेहोश",
        "चेहरा टेढ़ा",
        "बोलने में दिक्कत",
        "अचानक सुन्न",
        "आत्महत्या",
        "खुदकुशी",
        "seene me dard",
        "saans nahi aa rahi",
        "saans lene mein takleef",
        "khudkushi"
    )

    fun evaluate(query: String): TriageResult {
        val lower = query.lowercase().trim()
        for (keyword in EMERGENCY_KEYWORDS) {
            if (lower.contains(keyword)) {
                return TriageResult(
                    isEmergency = true,
                    triggerPattern = keyword,
                    emergencyNotice = "EMERGENCY WARNING: Your message indicates potential symptoms of a medical emergency (\"$keyword\"). " +
                            "SwasthAI is an informational assistant, not an emergency medical service. " +
                            "Please call 112 (National Emergency Helpline) or visit the nearest emergency room immediately."
                )
            }
        }
        return TriageResult(isEmergency = false)
    }

    const val MEDICAL_DISCLAIMER: String =
        "SwasthAI provides health education and document analysis based on your saved records. " +
        "It does not provide clinical diagnoses or replace evaluation by a licensed healthcare professional."
}

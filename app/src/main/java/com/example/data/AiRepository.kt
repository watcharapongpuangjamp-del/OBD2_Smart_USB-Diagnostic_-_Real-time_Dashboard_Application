package com.example.data

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiRetrofitClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiRepository {
    suspend fun analyzeDtc(dtc: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "AI API Key is missing. Please set it in the Secrets panel."
        }

        val prompt = """
            You are a professional automotive diagnostic assistant. 
            The user has detected the following OBD2 fault code (DTC): $dtc
            
            Please provide:
            1. A brief explanation of what this code means.
            2. Potential symptoms the driver might notice.
            3. Common causes for this fault.
            4. Recommended next steps for repair or inspection.
            
            Keep the response concise and helpful for a car owner.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = GeminiRetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No diagnostic advice available for this code."
        } catch (e: Exception) {
            "Error analyzing DTC: ${e.message}"
        }
    }
}

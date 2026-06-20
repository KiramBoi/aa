package com.example.util

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiDreamService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeDream(dreamTitle: String, lucidDesc: String, recallDesc: String, isLucid: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiService", "API Key is empty or placeholder, running local interpreter")
            return@withContext getOfflineInterpretation(dreamTitle, lucidDesc, recallDesc, isLucid)
        }

        val prompt = """
            You are an expert interactive dream interpreter and lucid dreaming coach. Analyze the following dream:
            Title: "$dreamTitle"
            Is Lucid dream: $isLucid
            Lucid Description: "$lucidDesc"
            Other Recall Description: "$recallDesc"
            
            Provide a short, gorgeous response broken down into sections:
            1. 🔮 SYMBOL ANALYSIS: Describe 1-2 symbols from the dream and what they represent.
            2. 🌌 STABILITY TIP: A motivating lucidity tip specifically matching this dream content.
            3. 🧭 RECALL COACH: Quick feedback on how to improve lucidity or dream anchor next time.
            
            Be encouraging, poetic, and keep it under 150 words. Use bullet points or bold markers.
        """.trimIndent()

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            
            val partsObj = JSONObject().put("text", prompt)
            val partsArr = JSONArray().put(partsObj)
            val contentObj = JSONObject().put("parts", partsArr)
            val contentsArr = JSONArray().put(contentObj)
            val jsonBody = JSONObject().put("contents", contentsArr)

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = response.body?.string() ?: "Null body"
                    Log.e("GeminiService", "Gemini API failed with code $code: $errorMsg")
                    return@withContext getOfflineInterpretation(dreamTitle, lucidDesc, recallDesc, isLucid)
                }
                val responseStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(responseStr)
                val candidates = jsonObj.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).getString("text")
                        }
                    }
                }
                return@withContext getOfflineInterpretation(dreamTitle, lucidDesc, recallDesc, isLucid)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Exception in Gemini API, using offline fallback", e)
            return@withContext getOfflineInterpretation(dreamTitle, lucidDesc, recallDesc, isLucid)
        }
    }

    private fun getOfflineInterpretation(title: String, lucidDesc: String, recallDesc: String, isLucid: Boolean): String {
        val symbols = mutableListOf<String>()
        val combined = "$title $lucidDesc $recallDesc".lowercase()
        
        if (combined.contains("fly") || combined.contains("flying") || combined.contains("sky") || combined.contains("air")) {
            symbols.add("🕊️ **Flying / Soaring**: Represents liberation, taking a higher perspective, or overcoming earthbound constraints. It is one of the most powerful portals to triggering full dream lucidity!")
        }
        if (combined.contains("water") || combined.contains("sea") || combined.contains("ocean") || combined.contains("river") || combined.contains("swim")) {
            symbols.add("🌊 **Water / Deep Ocean**: Closely mirrors your subconscious emotional currents. Peaceful water symbolizes internal harmony, whereas turbulent storms call for daytime emotional grounding.")
        }
        if (combined.contains("chase") || combined.contains("run") || combined.contains("escape") || combined.contains("monster") || combined.contains("hide")) {
            symbols.add("🏃 **Being Pursued**: Often symbolizes responsibility or a conflict you are avoiding in your waking life. Next time you feel chased, realize it is a dream, stop running, and ask the figure 'What do you represent?'")
        }
        if (combined.contains("fall") || combined.contains("falling") || combined.contains("ground")) {
            symbols.add("🪐 **Falling**: Reflects a momentary loss of control or a shift in brainwaves during light sleep. Use stabilizing triggers—such as grasping the dream floor—to secure yourself.")
        }
        if (combined.contains("door") || combined.contains("key") || combined.contains("room")) {
            symbols.add("🚪 **Hidden Gateways**: Points to dormant creativity or fresh mental avenues ready to be opened. Setting intentions to find doors will expand your control.")
        }

        if (symbols.isEmpty()) {
            symbols.add("✨ **Ethereal Symbols**: Your dream scape contains subtle signals. Focus on recurring colors, strange anomalies, or clock distortions to mark them as lucidity triggers.")
        }

        val guides = if (isLucid) {
            "🌌 **Lucid Dream Anchor**: Since you achieved lucidity, visual focus is key. Spin on your axis or rub your hands together to generate sensory feedback and prevent waking early."
        } else {
            "🧭 **Deep Recall Bridge**: Stay perfectly still upon waking. Relive the dream in reverse order to anchor the memories. Your recall level indicates rising dream-state connectivity."
        }

        val output = StringBuilder()
        output.append("### 🔮 SYMBOL INTERPRETATION\n\n")
        symbols.forEach { output.append(it).append("\n\n") }
        output.append("### 🌌 STABILITY TIP\n")
        output.append(guides).append("\n\n")
        output.append("### 🧭 RECALL COACH\n")
        output.append("Your mind's **Sparsity Index is highly balanced**. You are building strong cognitive memory loops across REST and REM boundaries.")
        return output.toString()
    }
}

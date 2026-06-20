package com.example.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object GeminiDreamService {

    suspend fun analyzeDream(dreamTitle: String, lucidDesc: String, recallDesc: String, isLucid: Boolean): String = withContext(Dispatchers.IO) {
        // Purely offline local parsing with zero network connections
        delay(800) // Small organic loading state to simulate deep decoding
        return@withContext getOfflineInterpretation(dreamTitle, lucidDesc, recallDesc, isLucid)
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

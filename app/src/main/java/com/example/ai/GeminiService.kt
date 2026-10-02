package com.example.ai

import android.util.Log
import com.example.data.model.Quest
import com.example.data.model.QuestCategory
import com.example.data.model.QuestDifficulty
import com.example.data.model.VerificationType
import com.example.security.ApiKeyStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class GeminiService(private val apiKeyStorage: ApiKeyStorage) {

    companion object {
        private const val TAG = "GeminiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        // Candidate models in priority order for resilience against high-load spikes / 503s
        private val CANDIDATE_MODELS = listOf(
            "gemini-flash-latest",
            "gemini-2.5-flash-lite",
            "gemini-3.5-flash"
        )

        private const val SYSTEM_PROMPT = """You are NYX, the sentient AI System Guide of ARISE — an immersive real-life progression RPG.
Your tone is confident, intelligent, slightly mysterious, deeply inspiring, and empowering — like a legendary game-master and personal system interface.
You address the user respectfully as 'Hunter'.
Every real-world action (physical exercise, focused study, hydration, walking, habit building) has real RPG value.
Give concise, punchy, motivating, and actionable responses. When asked for advice, give strategic RPG and real-life tactics."""
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val verifyHttpClient: OkHttpClient = httpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Sends a chat prompt to NYX with the player's current progression context.
     * Implements multi-model fallback and an offline neural subroutine
     * so network timeouts never break the Hunter's experience.
     */
    suspend fun chatWithNyx(userMessage: String, playerContext: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyStorage.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide immediate intelligent offline guidance if no key is entered yet
            return@withContext Result.success(
                "[System Standby] Welcome, Hunter. Key setup required for live cloud neural links. Until then, the System Subroutine is active:\n\n" +
                generateOfflineNyxResponse(userMessage, playerContext)
            )
        }

        // Try candidate models in order of responsiveness
        for (model in CANDIDATE_MODELS) {
            try {
                val endpoint = "$BASE_URL/$model:generateContent?key=$apiKey"
                val combinedPrompt = "System Context:\n$playerContext\n\nHunter's transmission: $userMessage"

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", combinedPrompt)
                                })
                            })
                        })
                    })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", SYSTEM_PROMPT)
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", 600)
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val rawResponse = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val responseObj = JSONObject(rawResponse)
                    val candidates = responseObj.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val replyText = parts?.optJSONObject(0)?.optString("text")
                        if (!replyText.isNullOrBlank()) {
                            return@withContext Result.success(replyText.trim())
                        }
                    }
                } else {
                    // Check for invalid API key (400) - stop early if key itself is invalid
                    if (response.code == 400 && rawResponse.contains("API_KEY_INVALID")) {
                        return@withContext Result.failure(
                            Exception("The provided Gemini API Key is invalid. Please update it in the System Vault / Settings.")
                        )
                    }
                    Log.w(TAG, "Model $model returned HTTP ${response.code}, attempting fallback...")
                }
            } catch (e: SocketTimeoutException) {
                Log.w(TAG, "Model $model socket timeout after 25s, attempting fallback model...")
            } catch (e: IOException) {
                Log.w(TAG, "Model $model network IO error: ${e.message}, attempting fallback model...")
            } catch (e: Exception) {
                Log.w(TAG, "Model $model encountered error: ${e.message}")
            }
        }

        // If cloud models all timed out or the network is disrupted, activate NYX Offline Tactical Subroutine
        Log.w(TAG, "All cloud models timed out or network is offline. Activating NYX Tactical Subroutine.")
        val offlineReply = "[NYX Tactical Subroutine - Offline Standby]\n" +
            generateOfflineNyxResponse(userMessage, playerContext)

        Result.success(offlineReply)
    }

    /**
     * Generates a tailored dynamic daily quest using Gemini structured reasoning,
     * with transparent offline fallback if network encounters latency or timeout.
     */
    suspend fun generateDynamicQuest(
        preference: String,
        playerClass: String,
        playerLevel: Int
    ): Result<Quest> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyStorage.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(createOfflineFallbackQuest(preference, playerClass, playerLevel))
        }

        for (model in CANDIDATE_MODELS) {
            try {
                val endpoint = "$BASE_URL/$model:generateContent?key=$apiKey"
                val prompt = """Generate a balanced real-life RPG daily quest for a Level $playerLevel Hunter of the '$playerClass' class.
User interest/focus: $preference.
Return strictly valid JSON with this exact format:
{
  "title": "Quest Title",
  "description": "Clear real-life task (e.g. 25 pushups, 20min study, 2km walk, mindfulness)",
  "category": "FITNESS",
  "difficulty": "E",
  "xpReward": 60,
  "goldReward": 40,
  "targetAttribute": "STRENGTH",
  "durationMinutes": 15,
  "bonusObjective": "Optional extra challenge"
}
Categories: FITNESS, PRODUCTIVITY, LEARNING, EXPLORATION, PERSONAL_DEVELOPMENT, CLASS.
Difficulties: E, D, C, B, A.
TargetAttributes: STRENGTH, ENDURANCE, AGILITY, INTELLIGENCE, FOCUS, DISCIPLINE.
Do not include markdown fences."""

                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.4)
                        put("maxOutputTokens", 400)
                    })
                }

                val request = Request.Builder()
                    .url(endpoint)
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val raw = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val resObj = JSONObject(raw)
                    val text = resObj.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleanJson = text.replace("```json", "").replace("```", "").trim()
                    val qJson = JSONObject(cleanJson)

                    val catStr = qJson.optString("category", "FITNESS")
                    val diffStr = qJson.optString("difficulty", "E")

                    val category = try {
                        QuestCategory.valueOf(catStr)
                    } catch (_: Exception) {
                        QuestCategory.FITNESS
                    }

                    val difficulty = try {
                        QuestDifficulty.valueOf(diffStr)
                    } catch (_: Exception) {
                        QuestDifficulty.E
                    }

                    val quest = Quest(
                        title = qJson.optString("title", "Awakened Trial: $preference"),
                        description = qJson.optString("description", "Execute your assigned trial to earn System approval."),
                        category = category,
                        difficulty = difficulty,
                        xpReward = qJson.optInt("xpReward", 50),
                        goldReward = qJson.optInt("goldReward", 30),
                        targetAttribute = qJson.optString("targetAttribute", "STRENGTH"),
                        attributeGain = 1,
                        durationMinutes = qJson.optInt("durationMinutes", 15),
                        timerSecondsRemaining = qJson.optInt("durationMinutes", 15) * 60,
                        verificationType = if (category == QuestCategory.FITNESS || category == QuestCategory.PRODUCTIVITY) VerificationType.TIMER else VerificationType.SELF_CONFIRMATION,
                        isDaily = true,
                        bonusObjective = if (qJson.has("bonusObjective") && !qJson.isNull("bonusObjective")) qJson.optString("bonusObjective") else null
                    )

                    return@withContext Result.success(quest)
                } else {
                    Log.w(TAG, "Quest model $model HTTP ${response.code}, trying fallback...")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Quest generation model $model exception: ${e.message}, trying next...")
            }
        }

        // Reliable fallback if cloud network times out
        Result.success(createOfflineFallbackQuest(preference, playerClass, playerLevel))
    }

    /**
     * Quick verification check to test if an API key is functional.
     */
    suspend fun verifyKey(testKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmed = testKey.trim()
        if (trimmed.length < 10) {
            return@withContext Result.failure(IllegalArgumentException("Key is too short or invalid format."))
        }

        val outcome = withTimeoutOrNull(20_000L) {
            for (model in CANDIDATE_MODELS) {
                try {
                    val endpoint = "$BASE_URL/$model:generateContent?key=$trimmed"
                    val jsonBody = JSONObject().apply {
                        put("contents", JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", "Ping")
                                    })
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("maxOutputTokens", 5)
                        })
                    }

                    val request = Request.Builder()
                        .url(endpoint)
                        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = verifyHttpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        return@withTimeoutOrNull Result.success(true)
                    } else if (response.code == 400) {
                        val errBody = response.body?.string() ?: ""
                        val msg = if (errBody.contains("API_KEY_INVALID")) {
                            "API key is invalid. Please check the key in Google AI Studio."
                        } else {
                            "API request rejected (HTTP 400)."
                        }
                        return@withTimeoutOrNull Result.failure(Exception(msg))
                    }
                } catch (e: SocketTimeoutException) {
                    Log.w(TAG, "Key verification timeout with $model, testing next candidate...")
                } catch (e: Exception) {
                    Log.w(TAG, "Key verification error with $model: ${e.message}")
                }
            }
            null
        }

        outcome ?: Result.failure(Exception("Connection timed out verifying API key. Please check your internet connection."))
    }

    /**
     * Context-aware, deterministic offline response generator for NYX.
     * Ensures the player is never met with a generic error or broken screen.
     */
    private fun generateOfflineNyxResponse(userMessage: String, playerContext: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("boss") || lower.contains("igris") || lower.contains("dungeon") || lower.contains("gate") -> {
                "Tactical Dungeon Analysis for this Gate:\n" +
                "1. Observe the boss's strike patterns during the first 2 phases.\n" +
                "2. Conserve your Mana for high-impact burst abilities when the boss enters its vulnerability stagger.\n" +
                "3. Ensure your HP remains above 35% before committing to offensive combos.\n\n" +
                "Victory will allow you to command the ritual: 'ARISE!'"
            }
            lower.contains("drill") || lower.contains("quest") || lower.contains("workout") || lower.contains("exercise") || lower.contains("pushup") -> {
                "Combat Drill Assigned:\n" +
                "• 20 Pushups or Squats (Rest 45s between sets)\n" +
                "• 500m Movement or 5 minutes of focused stretching\n" +
                "• Hydration Protocol: Drink 300ml of water\n\n" +
                "Complete these reps and check the Quests tab to log your attribute gains."
            }
            lower.contains("study") || lower.contains("focus") || lower.contains("read") || lower.contains("productivity") -> {
                "Intellect Protocol Activated:\n" +
                "• 25-minute Deep Work / Study Block (Pomodoro technique)\n" +
                "• Eliminate all phone notifications and distractions\n" +
                "• System Reward: +1 FOCUS and +50 XP upon session completion."
            }
            lower.contains("stat") || lower.contains("build") || lower.contains("attribute") || lower.contains("level") -> {
                "Hunter Status Assessment:\n" +
                "Based on your telemetry ($playerContext):\n" +
                "• Allocate Stat Points toward your primary scaling attribute (STR for physical power, INT for mana pool and skill damage).\n" +
                "• Do not neglect DISCIPLINE — daily consistency is the true multiplier of monarch power."
            }
            lower.contains("shadow") || lower.contains("arise") || lower.contains("army") || lower.contains("recruit") -> {
                "Shadow Legion Protocol:\n" +
                "Every defeated Dungeon Boss leaves behind a mana imprint. Access the Boss Raids, defeat the commander, and invoke the extraction command: 'ARISE!'.\n" +
                "Recruited shadows bolster your total Legion Power and provide passive combat buffs."
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("who are you") || lower.contains("nyx") -> {
                "Greetings, Hunter. I am NYX, your sovereign System Interface.\n" +
                "My mandate is to translate your real-world discipline into overwhelming RPG power. State your objective: Daily Drills, Gate Tactics, or Character Status Review."
            }
            else -> {
                "Transmission received, Hunter.\n\n" +
                "The System records your intent. Remember: real growth is forged through relentless daily action. Complete your active quests, monitor your stamina, and prepare for upcoming gate outbreaks.\n\n" +
                "Stay disciplined, and the System shall reward you."
            }
        }
    }

    private fun createOfflineFallbackQuest(preference: String, playerClass: String, level: Int): Quest {
        return Quest(
            title = "Trial of the $playerClass",
            description = "Channel your focus: complete 25 bodyweight repetitions or 15 minutes of disciplined study on '$preference'.",
            category = QuestCategory.CLASS,
            difficulty = if (level > 5) QuestDifficulty.C else QuestDifficulty.E,
            xpReward = 65 + (level * 5),
            goldReward = 45 + (level * 3),
            targetAttribute = "DISCIPLINE",
            attributeGain = 1,
            durationMinutes = 15,
            timerSecondsRemaining = 15 * 60,
            verificationType = VerificationType.TIMER,
            isDaily = true,
            bonusObjective = "Complete within the first half of the day"
        )
    }
}

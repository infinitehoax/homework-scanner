package com.example.data

import android.util.Log
import retrofit2.HttpException
import org.json.JSONObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.example.BuildConfig

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HomeworkRepository(private val homeworkDao: HomeworkDao) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listAdapter = moshi.adapter<List<ChatMessage>>(
        Types.newParameterizedType(List::class.java, ChatMessage::class.java)
    )

    val allSolutions: Flow<List<HomeworkEntity>> = homeworkDao.getAllSolutions()

    suspend fun getSolutionById(id: Int): HomeworkEntity? = withContext(Dispatchers.IO) {
        homeworkDao.getSolutionById(id)
    }

    suspend fun saveSolution(entity: HomeworkEntity): Int = withContext(Dispatchers.IO) {
        homeworkDao.insertSolution(entity).toInt()
    }

    suspend fun updateSolution(entity: HomeworkEntity) = withContext(Dispatchers.IO) {
        homeworkDao.updateSolution(entity)
    }

    suspend fun deleteSolution(entity: HomeworkEntity) = withContext(Dispatchers.IO) {
        homeworkDao.deleteSolution(entity)
    }

    suspend fun deleteSolutionById(id: Int) = withContext(Dispatchers.IO) {
        homeworkDao.deleteSolutionById(id)
    }

    // Serialize list of chat messages to JSON
    fun serializeHistory(history: List<ChatMessage>): String {
        return try {
            listAdapter.toJson(history) ?: "[]"
        } catch (e: Exception) {
            "[]"
        }
    }

    // Deserialize JSON to list of chat messages
    fun deserializeHistory(json: String): List<ChatMessage> {
        return try {
            listAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun solveHomework(
        subject: String,
        questionText: String,
        imageBase64: String?,
        imageUrl: String?,
        videoUrl: String?,
        audioUrl: String? = null,
        apiKey: String,
        tutorPersonality: String = "Balanced Tutor",
        explanationComplexity: String = "Detailed Step-by-Step"
    ): HomeworkEntity = withContext(Dispatchers.IO) {
        val finalApiKey = apiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }
        
        val personalityPromptSnippet = when(tutorPersonality) {
            "Socratic Guide" -> "Do not give the direct solution immediately. Act like Socrates: ask guiding sub-questions, lead the user with helpful hints, and encourage critical thinking."
            "Strict Examiner" -> "Adopt a rigorous, academic, and strict examiner tone. Highlight strict step-by-step evaluation rubrics and use professional grading terminology."
            "Casual Study Buddy" -> "Be a super chill, friendly study buddy! Use casual conversational language, humorous analogical comparisons, and friendly study emojis."
            else -> "Act as a helpful, professional, and encouraging AI personal tutor."
        }

        val complexityPromptSnippet = when(explanationComplexity) {
            "Quick Cheat Sheet" -> "Keep explanations ultra-concise, highlighting the final answer under 100 words with a rapid cheat-sheet checklist."
            "Focus Formulas" -> "Stress and list the critical scientific/mathematical formulas, explain the physical/logical derivations of equations, and highlight mathematical identities."
            else -> "Provide a detailed step-by-step walk-through, detailing intermediate calculations, steps, and core concepts."
        }

        // Formulate a robust prompt based on the subject category
        val subjectPromptSnippet = when (subject.lowercase()) {
            "math" -> "Solve this Math homework. Use beautiful LaTeX math notation for all equations and fractions. Write inline equations using single dollar signs or block equations using double dollar signs."
            "science" -> "Explain the Science question, highlighting chemical reactions, biological systems, or physical formulas in clean LaTeX formatting if equations are involved."
            "history" -> "Explain the History topic. List critical dates, timelines, prominent figures, background contexts, and historical consequences in clean tables or bulleted lists."
            "literature" -> "Provide a Literature analysis. Structure key points, textual quotes, author themes, and stylistic motifs in readable markdown blocks."
            "programming" -> "Solve this programming assignment. Output tidy code blocks with language-specific syntax formatting, explanation of logic, and time/space complexity analysis."
            else -> "Solve this assignment. Ensure proper markdown tables, bullet items, and elegant structure."
        }

        val styleInstruction = """
            Make sure your response uses markdown. You can render markdown tables using standard syntax.
            For inline math equations, wrap them in single dollar signs (e.g. ${"$"}E = mc^2${"$"}).
            For complex block equations, wrap them in double dollar signs (e.g. ${"$$"}x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}${"$$"}) or Bracket notation \[ \].
            Use generous markdown elements: Section Headers (##), lists, emphasis, and blockquotes.
        """.trimIndent()

        val systemInstructionText = """
            You are "PrepAlly", an expert homework tutor.
            Current Tutor Personality Preference: ${"$"}personalityPromptSnippet
            Current Explanation Style/Complexity: ${"$"}complexityPromptSnippet
            
            Always prioritize being deeply educational.
            Structure outputs beautifully with clearly distinguished sections.
            ${"$"}styleInstruction
        """.trimIndent()

        val promptParts = mutableListOf<Part>()
        
        val maxInlineBytes = 15 * 1024 * 1024 // 15 MB limit for JSON inline data
        
        // Include the video inline if provided natively
        if (videoUrl != null) {
            val videoFile = java.io.File(videoUrl)
            if (videoFile.exists()) {
                if (videoFile.length() > maxInlineBytes) {
                    throw Exception("Video is too large. Please record a shorter video (under 15MB).")
                }
                try {
                    val bytes = videoFile.readBytes()
                    val videoBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    promptParts.add(Part(inlineData = InlineData(mimeType = "video/mp4", data = videoBase64)))
                } catch (e: Throwable) {
                    Log.e("HomeworkRepository", "Failed to encode video for Gemini request", e)
                    throw Exception("Failed to process video.", e)
                }
            }
        }

        // Include the audio inline if provided natively
        if (audioUrl != null) {
            val audioFile = java.io.File(audioUrl)
            if (audioFile.exists()) {
                if (audioFile.length() > maxInlineBytes) {
                    throw Exception("Audio recording is too long. Please keep it under 15MB.")
                }
                try {
                    val bytes = audioFile.readBytes()
                    val audioBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    promptParts.add(Part(inlineData = InlineData(mimeType = "audio/mp4", data = audioBase64)))
                } catch (e: Throwable) {
                    Log.e("HomeworkRepository", "Failed to encode audio for Gemini request", e)
                    throw Exception("Failed to process audio.", e)
                }
            }
        }
        
        // Include the image context if uploaded (fallback if no video or as primary image)
        if (imageBase64 != null) {
            promptParts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = imageBase64)))
        }

        // Include the text query
        val qText = questionText.ifBlank {
            if (audioUrl != null) {
                "Please listen to the attached voice explanation and solve/explain the assignment step-by-step."
            } else {
                "Scanned or attached assignment"
            }
        }
        val textPrompt = "$subjectPromptSnippet\n\nQuestion:\n$qText"
        promptParts.add(Part(text = textPrompt))

        val request = GeminiRequest(
            contents = listOf(Content(role = "user", parts = promptParts)),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = finalApiKey,
                request = request
            )

            val answerText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("The AI could not generate an answer. The prompt may have been blocked by safety filters.")

            val entity = HomeworkEntity(
                subject = subject,
                questionText = questionText.ifEmpty { "Scanned assignment" },
                imageUrl = imageUrl,
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                solutionText = answerText,
                timestamp = System.currentTimeMillis()
            )

            entity
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e("HomeworkRepository", "API HTTP Error: $errorBody", e)
            var readableMessage = "Server code ${e.code()}"
            try {
                if (!errorBody.isNullOrBlank()) {
                    readableMessage = JSONObject(errorBody).getJSONObject("error").getString("message")
                }
            } catch (parseEx: Exception) { /* Fallback to standard message */ }
            throw Exception(readableMessage)
        } catch (e: Exception) {
            Log.e("HomeworkRepository", "Error solving homework", e)
            throw Exception(e.localizedMessage ?: "Unknown network error occurred.")
        }
    }

    suspend fun askFollowUp(
        entity: HomeworkEntity,
        followUpText: String,
        apiKey: String
    ): HomeworkEntity = withContext(Dispatchers.IO) {
        val finalApiKey = apiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }
        val currentHistory = deserializeHistory(entity.historyJson).toMutableList()

        // Append user's new question
        currentHistory.add(ChatMessage(isUser = true, text = followUpText))

        // Construct contents array for Gemini including original context and chat log
        val contents = mutableListOf<Content>()

        // 1. Initial Prompt context
        val initialUserParts = mutableListOf<Part>()
        if (entity.imageUrl != null) {
            // Reconstruct approximate image if base64 is not saved, or just rely on context
            // Note: In typical usage, the text conversation history is more than enough for follow-ups
        }
        initialUserParts.add(Part(text = "Subject: ${entity.subject}\nOriginal Question:\n${entity.questionText}"))
        contents.add(Content(role = "user", parts = initialUserParts))

        // 2. Initial Solution
        contents.add(Content(role = "model", parts = listOf(Part(text = entity.solutionText))))

        // 3. Subsequent Chat Logs
        // We exclude the last item because it is the current prompt we want to query
        for (i in 0 until currentHistory.size - 1) {
            val msg = currentHistory[i]
            contents.add(
                Content(
                    role = if (msg.isUser) "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            )
        }

        // 4. Current user follow-up prompt
        contents.add(Content(role = "user", parts = listOf(Part(text = followUpText))))

        val systemInstructionText = "You are continuing a tutoring session with the student. Answer their follow-up questions clearly while checking for their understanding."

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = "gemini-3.5-flash",
                apiKey = finalApiKey,
                request = request
            )

            val answerText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I couldn't generate a follow up response. Please try again."

            // Save our follow up response to chat history
            currentHistory.add(ChatMessage(isUser = false, text = answerText))

            val updatedEntity = entity.copy(
                historyJson = serializeHistory(currentHistory),
                timestamp = System.currentTimeMillis() // Bump the local timestamp or leave unchanged? Usually update timestamp for sorting
            )

            homeworkDao.updateSolution(updatedEntity)
            updatedEntity
        } catch (e: Exception) {
            Log.e("HomeworkRepository", "Error during follow up", e)
            throw e
        }
    }
}

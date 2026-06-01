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
        val finalApiKey = apiKey.ifEmpty { BuildConfig.RELAY_API_KEY }
        
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

        val systemInstructionText = """
            You are "PrepAlly", an expert homework tutor.
            Current Tutor Personality Preference: $personalityPromptSnippet
            Current Explanation Style/Complexity: $complexityPromptSnippet
            
            Always prioritize being deeply educational.
            Structure outputs beautifully with clearly distinguished sections.
        """.trimIndent()

        // 1. Process Video Base64 (if any)
        var videoBase64: String? = null
        if (videoUrl != null) {
            val videoFile = java.io.File(videoUrl)
            if (videoFile.exists()) {
                videoBase64 = android.util.Base64.encodeToString(videoFile.readBytes(), android.util.Base64.NO_WRAP)
            }
        }

        // 2. Process Audio Base64 (if any)
        var audioBase64: String? = null
        if (audioUrl != null) {
            val audioFile = java.io.File(audioUrl)
            if (audioFile.exists()) {
                audioBase64 = android.util.Base64.encodeToString(audioFile.readBytes(), android.util.Base64.NO_WRAP)
            }
        }

        val qText = questionText.ifBlank {
            if (audioUrl != null) "Please listen to the attached voice explanation and solve/explain."
            else "Scanned or attached assignment"
        }

        // 3. Create the Relay Request
        // IMPORTANT: The backend accepts ONLY ONE of image_b64, video_b64, or audio_b64.
        val request = RelayRequest(
            subject = subject,
            question = qText,
            system_instruction = systemInstructionText,
            image_b64 = if (videoBase64 == null && audioBase64 == null) imageBase64 else null,
            video_b64 = videoBase64,
            audio_b64 = audioBase64
        )

        try {
            val requestStartTime = System.currentTimeMillis()
            
            // Generate a clean log of what's being sent
            val payloadDetails = StringBuilder()
            payloadDetails.append("Subject: ${request.subject}\n")
            payloadDetails.append("Question: ${request.question}\n")
            payloadDetails.append("System Instruction: ${request.system_instruction}\n")
            payloadDetails.append("Has Image: ${request.image_b64 != null}\n")
            payloadDetails.append("Has Video: ${request.video_b64 != null}\n")
            payloadDetails.append("Has Audio: ${request.audio_b64 != null}")
            
            com.example.util.ApiLogger.info("RELAY_REQUEST", "Sending payload to PrepAlly Relay.", payloadDetails.toString())
            
            val response = RetrofitClient.service.solveHomework(
                apiKey = finalApiKey,
                request = request
            )
            
            if (!response.success || response.answer == null) {
                com.example.util.ApiLogger.error("RELAY_ERROR", "Server returned false success or null answer", response.error_msg)
                throw Exception(response.error_msg ?: "AI services failed to return a response.")
            }

            val duration = System.currentTimeMillis() - requestStartTime
            com.example.util.ApiLogger.info("RELAY_RESPONSE", "Success using provider: ${response.provider_used} in ${duration}ms", "Answer Preview:\n${response.answer.take(200)}...")

            HomeworkEntity(
                subject = subject,
                questionText = questionText.ifEmpty { "Scanned assignment" },
                imageUrl = imageUrl,
                videoUrl = videoUrl,
                audioUrl = audioUrl,
                solutionText = response.answer,
                timestamp = System.currentTimeMillis()
            )
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
        val finalApiKey = apiKey.ifEmpty { BuildConfig.RELAY_API_KEY }
        val currentHistory = deserializeHistory(entity.historyJson).toMutableList()

        // 1. Build string representation of chat history
        val historyBuilder = StringBuilder()
        historyBuilder.append("Original Question: ${entity.questionText}\n")
        historyBuilder.append("Original Solution: ${entity.solutionText}\n\n")
        historyBuilder.append("--- Chat History ---\n")
        
        currentHistory.forEach { msg ->
            val role = if (msg.isUser) "Student" else "Tutor"
            historyBuilder.append("$role: ${msg.text}\n")
        }
        
        historyBuilder.append("\nStudent's new follow-up question: $followUpText")

        currentHistory.add(ChatMessage(isUser = true, text = followUpText))

        val request = RelayRequest(
            subject = entity.subject,
            question = historyBuilder.toString(),
            system_instruction = "You are continuing a tutoring session. Use the provided Chat History for context. Answer the student's new follow-up question clearly.",
            image_b64 = null, video_b64 = null, audio_b64 = null
        )

        try {
            com.example.util.ApiLogger.info("RELAY_FOLLOWUP_REQUEST", "Sending follow up chat via Relay.", "Prompt:\n$followUpText")
            val requestStartTime = System.currentTimeMillis()
            val response = RetrofitClient.service.solveHomework(finalApiKey, request)

            if (!response.success || response.answer == null) {
                com.example.util.ApiLogger.error("RELAY_FOLLOWUP_ERROR", "Failed to generate follow-up.", response.error_msg)
                throw Exception(response.error_msg ?: "Failed to generate follow-up.")
            }

            val duration = System.currentTimeMillis() - requestStartTime
            com.example.util.ApiLogger.info("RELAY_FOLLOWUP_RESPONSE", "Success using provider: ${response.provider_used} in ${duration}ms", "Answer Preview:\n${response.answer.take(200)}...")

            currentHistory.add(ChatMessage(isUser = false, text = response.answer))

            val updatedEntity = entity.copy(
                historyJson = serializeHistory(currentHistory),
                timestamp = System.currentTimeMillis()
            )

            homeworkDao.updateSolution(updatedEntity)
            updatedEntity
        } catch (e: Exception) {
            throw e
        }
    }
}

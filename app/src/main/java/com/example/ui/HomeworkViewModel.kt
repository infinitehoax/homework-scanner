package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.media.MediaMetadataRetriever
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class HomeworkViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HomeworkDatabase.getDatabase(application)
    private val repository = HomeworkRepository(database.homeworkDao())

    val allSolutions: StateFlow<List<HomeworkEntity>> = repository.allSolutions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentSolution = MutableStateFlow<HomeworkEntity?>(null)
    val currentSolution: StateFlow<HomeworkEntity?> = _currentSolution.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Key settings and customizations (tutorPersonality, explanationComplexity)
    private val _tutorPersonality = MutableStateFlow("Balanced Tutor")
    val tutorPersonality = _tutorPersonality.asStateFlow()

    private val _explanationComplexity = MutableStateFlow("Detailed Step-by-Step")
    val explanationComplexity = _explanationComplexity.asStateFlow()

    fun updatePersonality(personality: String) {
        _tutorPersonality.value = personality
    }

    fun updateComplexity(complexity: String) {
        _explanationComplexity.value = complexity
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                allSolutions.value.forEach {
                    repository.deleteSolution(it)
                }
                _currentSolution.value = null
            } catch (e: Exception) {
                Log.e("HomeworkViewModel", "Failed to clear solutions", e)
            }
        }
    }

    // To handle optional custom API Key setting
    private val _userApiKey = MutableStateFlow("")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    data class ActiveScan(
        val id: String = java.util.UUID.randomUUID().toString(),
        val subject: String,
        val timestamp: Long = System.currentTimeMillis(),
        val status: MutableStateFlow<String> = MutableStateFlow("Queued"),
        val isError: MutableStateFlow<Boolean> = MutableStateFlow(false)
    )

    private val _activeScans = MutableStateFlow<List<ActiveScan>>(emptyList())
    val activeScans: StateFlow<List<ActiveScan>> = _activeScans.asStateFlow()

    fun updateApiKey(newKey: String) {
        _userApiKey.value = newKey
    }

    fun getActiveApiKey(): String {
        return _userApiKey.value.ifEmpty { BuildConfig.RELAY_API_KEY }
    }

    fun isApiKeyAvailable(): Boolean {
        return getActiveApiKey().trim().isNotEmpty() && getActiveApiKey() != "MY_GEMINI_API_KEY"
    }

    fun selectSolution(entity: HomeworkEntity?) {
        _currentSolution.value = entity
        _errorMessage.value = null
    }

    private var activeApiJob: Job? = null

    private val _queueStatus = MutableStateFlow<String?>(null)
    val queueStatus: StateFlow<String?> = _queueStatus.asStateFlow()

    fun cancelActiveRequest(scanId: String) {
        // Can optionally cancel the job if we mapped jobs to ids, but for now just remove it
        _activeScans.value = _activeScans.value.filter { it.id != scanId }
    }

    fun solveHomework(
        subject: String,
        questionText: String,
        imageUri: Uri?,
        videoUri: Uri? = null,
        audioPath: String? = null
    ) {
        val activeScan = ActiveScan(subject = subject)
        _activeScans.value = listOf(activeScan) + _activeScans.value

        viewModelScope.launch {
            activeScan.status.value = "Preparing media files..."
            
            try {
                var imageBase64: String? = null
                var localImagePath: String? = null
                var localVideoPath: String? = null

                // 1. Process Image if supplied
                if (imageUri != null) {
                    activeScan.status.value = "Processing image..."
                    val contentResolver = getApplication<Application>().contentResolver
                    val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (bitmap != null) {
                        val scaled = scaleBitmapIfNeeded(bitmap)
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        val bytes = out.toByteArray()
                        imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                        val cacheFile = File(
                            getApplication<Application>().cacheDir,
                            "hw_scan_${System.currentTimeMillis()}.jpg"
                        )
                        cacheFile.outputStream().use { fos ->
                            fos.write(bytes)
                        }
                        localImagePath = cacheFile.absolutePath
                    }
                }

                // 2. Process Video inputs if supplied by copying locally and extracting visual frames
                if (videoUri != null) {
                    activeScan.status.value = "Processing video..."
                    val cr = getApplication<Application>().contentResolver
                    val cacheVideoFile = File(
                        getApplication<Application>().cacheDir,
                        "hw_video_${System.currentTimeMillis()}.mp4"
                    )
                    try {
                        cr.openInputStream(videoUri)?.use { input ->
                            cacheVideoFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        localVideoPath = cacheVideoFile.absolutePath
                    } catch (e: Exception) {
                        Log.e("HomeworkViewModel", "Failed to copy video", e)
                    }

                    // Extract static frame at 1s timestamp via retriever
                    val extractedBitmap = extractFrameFromVideo(getApplication(), videoUri)
                    if (extractedBitmap != null) {
                        val scaled = scaleBitmapIfNeeded(extractedBitmap)
                        val out = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        val bytes = out.toByteArray()
                        
                        // We use the extracted frame as primary imageBase64 context
                        imageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                        if (localImagePath == null) {
                            val cacheFile = File(
                                getApplication<Application>().cacheDir,
                                "hw_scan_${System.currentTimeMillis()}.jpg"
                            )
                            cacheFile.outputStream().use { fos ->
                                fos.write(bytes)
                            }
                            localImagePath = cacheFile.absolutePath
                        }
                    }
                }

                if (questionText.trim().isEmpty() && imageBase64 == null && audioPath == null) {
                    activeScan.isError.value = true
                    activeScan.status.value = "Missing input: no question text or media."
                    return@launch
                }

                var attempt = 1
                var isSuccess = false
                val maxRetries = 10

                while (isActive && !isSuccess && attempt <= maxRetries) {
                    try {
                        activeScan.status.value = "Uploading to network and analyzing (Attempt $attempt)..."
                        val resultEntity = repository.solveHomework(
                            subject = subject,
                            questionText = questionText,
                            imageBase64 = imageBase64,
                            imageUrl = localImagePath,
                            videoUrl = localVideoPath,
                            audioUrl = audioPath,
                            apiKey = getActiveApiKey(),
                            tutorPersonality = _tutorPersonality.value,
                            explanationComplexity = _explanationComplexity.value
                        )

                        // Save result to Room Database
                        activeScan.status.value = "Saving result..."
                        val solvedId = repository.saveSolution(resultEntity)
                        val savedEntity = resultEntity.copy(id = solvedId)
                        
                        // Clean up active scan as it's finished successfully
                        _activeScans.value = _activeScans.value.filter { it.id != activeScan.id }
                        isSuccess = true
                    } catch (e: Exception) {
                        val errorMsg = e.localizedMessage ?: e.message ?: ""
                        if (errorMsg.contains("429") || errorMsg.contains("quota", ignoreCase = true) || errorMsg.contains("rate limit", ignoreCase = true)) {
                            val delaySeconds = (2 * attempt).coerceAtMost(10)
                            activeScan.status.value = "Server busy (429). Retrying in ${delaySeconds}s..."
                            delay(delaySeconds * 1000L)
                            attempt++
                        } else {
                            activeScan.isError.value = true
                            activeScan.status.value = "Server error: $errorMsg"
                            break
                        }
                    }
                }

                if (!isSuccess && attempt > maxRetries) {
                    activeScan.isError.value = true
                    activeScan.status.value = "Failed after $maxRetries attempts."
                }

            } catch (e: Throwable) {
                Log.e("HomeworkViewModel", "Failed to solve homework", e)
                activeScan.isError.value = true
                activeScan.status.value = "Exception: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    private fun extractFrameFromVideo(context: Application, videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            // Extract frame at 1s in microseconds (1,000,000)
            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            Log.e("HomeworkViewModel", "Failed to extract frame from video", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {}
        }
    }

    fun deleteSolution(entity: HomeworkEntity) {
        viewModelScope.launch {
            repository.deleteSolution(entity)
            if (_currentSolution.value?.id == entity.id) {
                _currentSolution.value = null
            }
        }
    }

    fun toggleFavorite(entity: HomeworkEntity) {
        viewModelScope.launch {
            val updated = entity.copy(isFavorite = !entity.isFavorite)
            repository.updateSolution(updated)
            if (_currentSolution.value?.id == entity.id) {
                _currentSolution.value = updated
            }
        }
    }

    fun askFollowUp(followUpText: String) {
        val current = _currentSolution.value ?: return
        if (followUpText.trim().isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val updated = repository.askFollowUp(
                    entity = current,
                    followUpText = followUpText,
                    apiKey = getActiveApiKey()
                )
                _currentSolution.value = updated
            } catch (e: Exception) {
                Log.e("HomeworkViewModel", "Failed with follow up chat", e)
                _errorMessage.value = "Failed to continue explanation: ${e.localizedMessage ?: e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearActiveSolution() {
        _currentSolution.value = null
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDimension = 1024
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / aspectRatio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * aspectRatio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // Load Chat history for active solution
    fun getCurrentSolutionHistory(): List<ChatMessage> {
        val current = _currentSolution.value ?: return emptyList()
        return repository.deserializeHistory(current.historyJson)
    }
}

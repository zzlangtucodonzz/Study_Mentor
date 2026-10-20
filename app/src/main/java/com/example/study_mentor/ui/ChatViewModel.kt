package com.example.study_mentor.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.study_mentor.OpenAIApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

// ─── Data Models ────────────────────────────────────────────────────────────

/**
 * Represents a single message in the chat thread.
 * @param role  Either "user" or "ai".
 * @param text  The message content.
 */
data class ChatMessage(
    val role: String,
    val text: String
)

/**
 * The complete UI state for the chat screen.
 * @param messages            Ordered list of chat messages (oldest first).
 * @param isLoading           True while awaiting the initial AI response token.
 * @param errorMessage        Non-null when the last API call encountered an error.
 * @param selectedImageUri    URI of the image chosen from gallery for preview.
 * @param selectedImageBase64 Compressed Base64 representation of the selected image.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedImageUri: Uri? = null,
    val selectedImageBase64: String? = null
)

// ─── ViewModel ───────────────────────────────────────────────────────────────

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var isStreaming = false

    /**
     * Handles gallery image selection. Decodes the image, downscales it to max 1024x1024,
     * compresses it as JPEG at 80% quality, and stores its Base64 string on Dispatchers.IO.
     */
    fun onImageSelected(uri: Uri?, context: Context) {
        if (uri == null) {
            clearSelectedImage()
            return
        }

        // Show thumbnail immediately in UI while compression runs in background
        _uiState.update { it.copy(selectedImageUri = uri, selectedImageBase64 = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val base64 = processAndCompressImage(context, uri)
                _uiState.update { it.copy(selectedImageBase64 = base64) }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to compress and encode image", e)
            }
        }
    }

    /** Resets the selected image URI and Base64 string. */
    fun clearSelectedImage() {
        _uiState.update { it.copy(selectedImageUri = null, selectedImageBase64 = null) }
    }

    /**
     * Safely reads, downsamples, scales (max 1024x1024), and compresses an image to JPEG 80%.
     */
    private fun processAndCompressImage(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver

        // 1. Decode bounds first to calculate inSampleSize and prevent OutOfMemory (OOM)
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        val maxDim = 1024
        var inSampleSize = 1
        if (rawHeight > maxDim || rawWidth > maxDim) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= maxDim && (halfWidth / inSampleSize) >= maxDim) {
                inSampleSize *= 2
            }
        }

        // 2. Decode the downsampled bitmap
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val sampledBitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return null

        // 3. Scale proportionally so max(width, height) <= 1024
        val currentWidth = sampledBitmap.width
        val currentHeight = sampledBitmap.height
        val longestSide = maxOf(currentWidth, currentHeight)

        val finalBitmap = if (longestSide > maxDim) {
            val scale = maxDim.toFloat() / longestSide
            val targetWidth = (currentWidth * scale).toInt()
            val targetHeight = (currentHeight * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
            if (scaled != sampledBitmap) {
                sampledBitmap.recycle()
            }
            scaled
        } else {
            sampledBitmap
        }

        // 4. Compress to JPEG at 80% quality
        val outputStream = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        finalBitmap.recycle()

        // 5. Encode byte array to Base64 without line wraps
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Sends a user prompt to the AI and streams the reply token-by-token
     * into the UI state in real time.
     *
     * @param prompt  The question or instruction typed by the user.
     */
    fun sendMessage(prompt: String) {
        val base64ToSend = _uiState.value.selectedImageBase64
        val trimmedPrompt = prompt.trim()

        if ((trimmedPrompt.isBlank() && base64ToSend == null) || isStreaming) return

        // Clear thumbnail immediately from the input area
        clearSelectedImage()

        // Optional UI tweak: Indicate in chat history that an image was attached
        val bubbleText = if (base64ToSend != null) {
            if (trimmedPrompt.isNotBlank()) "[Image attached]\n$trimmedPrompt" else "[Image attached]"
        } else {
            trimmedPrompt
        }

        // 1. Immediately append the user message and start the thinking indicator
        val userMessage = ChatMessage(role = "user", text = bubbleText)
        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage,
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            isStreaming = true
            var isFirstChunk = true
            val responseBuilder = StringBuilder()

            try {
                // 2. Collect streaming text chunks directly as Flow<String> (with optional image)
                OpenAIApiClient.askQuestion(trimmedPrompt, base64ToSend)
                    .collect { chunkText ->
                        // Strip asterisks so raw Markdown doesn't leak into the UI
                        val cleanDelta = chunkText.replace("*", "")
                        if (cleanDelta.isEmpty()) return@collect

                        responseBuilder.append(cleanDelta)
                        val accumulatedText = responseBuilder.toString()

                        if (isFirstChunk) {
                            isFirstChunk = false
                            // First token received: dismiss the thinking spinner and append the AI bubble
                            val aiMessage = ChatMessage(role = "ai", text = accumulatedText)
                            _uiState.update { current ->
                                current.copy(
                                    messages = current.messages + aiMessage,
                                    isLoading = false
                                )
                            }
                        } else {
                            // Subsequent chunks: update the AI bubble text character-by-character
                            _uiState.update { current ->
                                if (current.messages.isEmpty()) return@update current
                                val updatedList = current.messages.toMutableList()
                                val lastIndex = updatedList.lastIndex
                                updatedList[lastIndex] = updatedList[lastIndex].copy(text = accumulatedText)
                                current.copy(messages = updatedList)
                            }
                        }
                    }

                // If the stream finished without emitting any tokens
                if (isFirstChunk) {
                    val emptyNotice = ChatMessage(
                        role = "ai",
                        text = "Sorry, I received an empty response. Please try again."
                    )
                    _uiState.update { current ->
                        current.copy(
                            messages = current.messages + emptyNotice,
                            isLoading = false
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("OpenAI_Debug", "Streaming Error", e)
                val errorText = "Debug Error: ${e.message ?: "Connection failed"}"
                _uiState.update { current ->
                    if (isFirstChunk) {
                        current.copy(
                            messages = current.messages + ChatMessage(role = "ai", text = errorText),
                            isLoading = false,
                            errorMessage = errorText
                        )
                    } else {
                        current.copy(
                            isLoading = false,
                            errorMessage = errorText
                        )
                    }
                }
            } finally {
                isStreaming = false
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Clears any displayed error message. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

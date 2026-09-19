package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class ReceiptOcrHelper {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTextFromBitmap(bitmap: Bitmap): Result<String> = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()
            val text = visionText.text.trim()
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractTextFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(image).await()
            val text = visionText.text.trim()
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

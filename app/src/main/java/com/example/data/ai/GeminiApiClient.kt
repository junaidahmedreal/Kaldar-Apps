package com.example.data.ai

import android.util.Log
import com.example.data.local.entity.ReceiptItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        private const val TAG = "GeminiApiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        // Try gemini-3.5-flash first per AI Studio skill guidelines, with fallback to gemini-1.5-flash
        private const val PRIMARY_MODEL = "gemini-3.5-flash"
        private const val FALLBACK_MODEL = "gemini-1.5-flash"

        val SYSTEM_PROMPT = """
            You are an expert offline-first financial receipt parser.
            Extract expense details from the receipt OCR text into structured JSON.
            Return ONLY a valid JSON object without any Markdown formatting or backticks.
            Expected JSON format:
            {
              "merchant": "Store or merchant name",
              "date": "YYYY-MM-DD",
              "time": "HH:MM",
              "totalAmount": 12.50,
              "currency": "$",
              "category": "Food",
              "paymentMethod": "Cash",
              "tax": 1.25,
              "discount": 0.0,
              "invoiceNumber": "INV-102",
              "items": [
                {
                  "name": "Item Description",
                  "quantity": 1.0,
                  "price": 6.25
                }
              ]
            }
            Category MUST be one of: Food, Groceries, Fuel, Medical, Travel, Shopping, Utilities, Entertainment, Office, Other.
            Ensure totalAmount is a numeric value (Double).
        """.trimIndent()
    }

    suspend fun parseReceiptText(rawText: String, apiKey: String): Result<ParsedReceiptData> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Gemini API key is empty"))
        }

        val requestPayload = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            val textPart = JSONObject().apply {
                put("text", "$SYSTEM_PROMPT\n\nRECEIPT OCR TEXT:\n$rawText")
            }
            partsArray.put(textPart)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)

            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.1)
                put("topP", 0.95)
            }
            put("generationConfig", genConfig)
        }

        // Try primary model, fallback if needed
        var responseResult = callModelApi(PRIMARY_MODEL, apiKey, requestPayload)
        if (responseResult.isFailure) {
            Log.w(TAG, "Primary model call failed, trying fallback: ${responseResult.exceptionOrNull()?.message}")
            responseResult = callModelApi(FALLBACK_MODEL, apiKey, requestPayload)
        }

        responseResult.fold(
            onSuccess = { responseText ->
                val parsed = parseJsonResponse(responseText, rawText)
                Result.success(parsed)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private fun callModelApi(model: String, apiKey: String, payload: JSONObject): Result<String> {
        val endpoint = "$BASE_URL/$model:generateContent?key=$apiKey"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(Exception("Gemini HTTP Error $code: $bodyString"))
            }

            // Extract candidate text
            val json = JSONObject(bodyString)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (text.isBlank()) {
                Result.failure(Exception("Empty response received from Gemini"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseJsonResponse(responseText: String, rawText: String): ParsedReceiptData {
        // Strip any markdown code blocks
        var cleaned = responseText.trim()
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").trim()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.removeSuffix("```").trim()
        }

        val firstBrace = cleaned.indexOf('{')
        val lastBrace = cleaned.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1)
        }

        return try {
            val json = JSONObject(cleaned)
            val merchant = json.optString("merchant", "Unknown Merchant").ifBlank { "Unknown Merchant" }
            val date = json.optString("date", "")
            val time = json.optString("time", "")
            val totalAmount = json.optDouble("totalAmount", 0.0)
            val currency = json.optString("currency", "$")
            val category = json.optString("category", "Other")
            val paymentMethod = json.optString("paymentMethod", "Cash")
            val tax = json.optDouble("tax", 0.0)
            val discount = json.optDouble("discount", 0.0)
            val invoiceNumber = json.optString("invoiceNumber", "")

            val itemsList = mutableListOf<ReceiptItem>()
            val itemsJsonArray = json.optJSONArray("items")
            if (itemsJsonArray != null) {
                for (i in 0 until itemsJsonArray.length()) {
                    val itemObj = itemsJsonArray.optJSONObject(i)
                    if (itemObj != null) {
                        itemsList.add(
                            ReceiptItem(
                                name = itemObj.optString("name", "Item"),
                                quantity = itemObj.optDouble("quantity", 1.0),
                                price = itemObj.optDouble("price", 0.0)
                            )
                        )
                    }
                }
            }

            ParsedReceiptData(
                merchant = merchant,
                date = date,
                time = time,
                totalAmount = totalAmount,
                currency = currency,
                category = category,
                paymentMethod = paymentMethod,
                tax = tax,
                discount = discount,
                invoiceNumber = invoiceNumber,
                items = itemsList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini JSON output: ${e.message}, falling back to rule-based")
            RuleBasedCategorizer.parseOfflineFallback(rawText)
        }
    }
}

package com.example.myjarvis

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

object GPTManager {
    private const val API_KEY = "YOUR_OPENAI_API_KEY"
    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    fun getResponse(prompt: String): String {
        val json = """
        {
            "model": "gpt-3.5-turbo",
            "messages": [{"role": "user", "content": "$prompt"}]
        }
        """.trimIndent()

        val requestBody = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(requestBody)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val jsonResponse = JSONObject(response.body?.string() ?: "")
            jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        } catch (e: Exception) {
            "उत्तर प्राप्त करने में त्रुटि"
        }
    }
}

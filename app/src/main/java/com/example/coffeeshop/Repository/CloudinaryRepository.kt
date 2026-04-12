package com.example.coffeeshop.Repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

/**
 * CloudinaryRepository — Upload ảnh lên Cloudinary.
 * Đồng bộ với cloudinaryService.ts trên React admin dashboard.
 *
 * Cloud Name: duaub6imq
 * Upload Preset: coffeeshop_preset (Unsigned)
 * Tự động tối ưu URL: w_500,q_auto,f_auto
 */
class CloudinaryRepository {

    private val client = OkHttpClient()

    /**
     * Upload file ảnh lên Cloudinary.
     * @return URL đã tối ưu (w_500,q_auto,f_auto) hoặc null nếu thất bại
     */
    suspend fun uploadImage(imageFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.name,
                    imageFile.asRequestBody("image/*".toMediaType()))
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build()

            val request = Request.Builder()
                .url("$BASE_URL/$CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Upload failed: $errorBody")
                return@withContext Result.failure(Exception("Upload thất bại: $errorBody"))
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response"))

            val json = JSONObject(responseBody)
            val secureUrl = json.getString("secure_url")

            // Tối ưu URL giống cloudinaryService.ts
            val optimizedUrl = optimizeUrl(secureUrl)

            Log.d(TAG, "Upload success: $optimizedUrl")
            Result.success(optimizedUrl)

        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Chèn transformation vào Cloudinary URL — giống hệt optimizeCloudinaryUrl()
     * trong cloudinaryService.ts
     */
    private fun optimizeUrl(secureUrl: String): String {
        return secureUrl.replace(
            "/image/upload/",
            "/image/upload/w_500,q_auto,f_auto/"
        )
    }

    companion object {
        private const val TAG = "CloudinaryRepository"
        private const val BASE_URL = "https://api.cloudinary.com/v1_1"
        private const val CLOUD_NAME = "duaub6imq"
        private const val UPLOAD_PRESET = "coffeeshop_preset"
    }
}

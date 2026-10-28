package com.swasthai.app.ai

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

enum class ModelArtifactDownloadStatus {
    PENDING,
    RUNNING,
    PAUSED,
    READY,
    FAILED,
    MISSING
}

data class ModelArtifactDownload(
    val status: ModelArtifactDownloadStatus,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = GemmaModelArtifactManager.EXPECTED_MODEL_BYTES,
    val message: String? = null
) {
    val percent: Int?
        get() = totalBytes.takeIf { it > 0 }?.let { ((downloadedBytes * 100) / it).toInt().coerceIn(0, 100) }
}

/** Downloads an immutable, public LiteRT-LM Gemma build through Android's durable DownloadManager. */
class GemmaModelArtifactManager(context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val modelFile: File
        get() = File(
            appContext.getExternalFilesDir(null) ?: appContext.filesDir,
            Gemma4ToolCallingModel.MODEL_FILE_NAME
        )

    fun enqueue(): Long {
        val currentId = preferences.getLong(KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_ID)
        if (currentId != INVALID_DOWNLOAD_ID) {
            val current = downloadManager.query(DownloadManager.Query().setFilterById(currentId))
            current.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) return currentId
            }
        }

        modelFile.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) { "Could not prepare model storage." }
        }
        val availableBytes = android.os.StatFs(modelFile.parentFile?.absolutePath ?: appContext.filesDir.absolutePath).availableBytes
        check(availableBytes >= EXPECTED_MODEL_BYTES + MINIMUM_FREE_SPACE_AFTER_DOWNLOAD) {
            "At least 3.1 GB of free storage is required for the Gemma 4 download."
        }

        val request = DownloadManager.Request(Uri.parse(MODEL_DOWNLOAD_URL))
            .setTitle("Gemma 4 E2B for SwasthAI")
            .setDescription("Downloading the 2.59 GB on-device model")
            .setMimeType("application/octet-stream")
            .setDestinationInExternalFilesDir(appContext, null, Gemma4ToolCallingModel.MODEL_FILE_NAME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
        val id = downloadManager.enqueue(request)
        preferences.edit().putLong(KEY_DOWNLOAD_ID, id).apply()
        return id
    }

    fun activeDownloadId(): Long? = preferences.getLong(KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_ID)
        .takeIf { it != INVALID_DOWNLOAD_ID }

    suspend fun query(downloadId: Long): ModelArtifactDownload = withContext(Dispatchers.IO) {
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            ?: return@withContext ModelArtifactDownload(ModelArtifactDownloadStatus.MISSING)
        cursor.use {
            if (!it.moveToFirst()) return@withContext ModelArtifactDownload(ModelArtifactDownloadStatus.MISSING)
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0)
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { value -> value > 0 }
                ?: EXPECTED_MODEL_BYTES
            when (status) {
                DownloadManager.STATUS_PENDING -> ModelArtifactDownload(ModelArtifactDownloadStatus.PENDING, downloaded, total)
                DownloadManager.STATUS_RUNNING -> ModelArtifactDownload(ModelArtifactDownloadStatus.RUNNING, downloaded, total)
                DownloadManager.STATUS_PAUSED -> ModelArtifactDownload(ModelArtifactDownloadStatus.PAUSED, downloaded, total)
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val verifiedVersion = preferences.getString(KEY_VERIFIED_VERSION, null)
                    if (verifiedVersion == MODEL_REVISION && modelFile.length() == EXPECTED_MODEL_BYTES) {
                        preferences.edit().remove(KEY_DOWNLOAD_ID).apply()
                        return@withContext ModelArtifactDownload(ModelArtifactDownloadStatus.READY, total, total)
                    }
                    val verified = verifyModelFile()
                    if (verified) {
                        preferences.edit()
                            .putString(KEY_VERIFIED_VERSION, MODEL_REVISION)
                            .remove(KEY_DOWNLOAD_ID)
                            .apply()
                        ModelArtifactDownload(ModelArtifactDownloadStatus.READY, total, total)
                    } else {
                        modelFile.delete()
                        preferences.edit().remove(KEY_DOWNLOAD_ID).remove(KEY_VERIFIED_VERSION).apply()
                        ModelArtifactDownload(
                            ModelArtifactDownloadStatus.FAILED,
                            downloaded,
                            total,
                            "Downloaded model failed its SHA-256 check and was deleted. Try downloading again."
                        )
                    }
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    preferences.edit().remove(KEY_DOWNLOAD_ID).apply()
                    ModelArtifactDownload(
                        ModelArtifactDownloadStatus.FAILED,
                        downloaded,
                        total,
                        "Model download failed (DownloadManager reason $reason). Check network and free storage, then retry."
                    )
                }
                else -> ModelArtifactDownload(ModelArtifactDownloadStatus.MISSING, downloaded, total)
            }
        }
    }

    fun cancel(downloadId: Long) {
        downloadManager.remove(downloadId)
        preferences.edit().remove(KEY_DOWNLOAD_ID).apply()
        modelFile.delete()
    }

    private suspend fun verifyModelFile(): Boolean = withContext(Dispatchers.IO) {
        if (!modelFile.isFile || modelFile.length() != EXPECTED_MODEL_BYTES) return@withContext false
        val digest = MessageDigest.getInstance("SHA-256")
        modelFile.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte) } == MODEL_SHA256
    }

    companion object {
        const val EXPECTED_MODEL_BYTES = 2_588_147_712L
        const val MODEL_DOWNLOAD_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/1caa527c1e88b55f046fcd8d2b4265606680663d/gemma-4-E2B-it.litertlm?download=true"
        private const val MODEL_REVISION = "1caa527c1e88b55f046fcd8d2b4265606680663d"
        private const val MODEL_SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
        private const val PREFERENCES_NAME = "gemma_model_artifact"
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_VERIFIED_VERSION = "verified_revision"
        private const val INVALID_DOWNLOAD_ID = -1L
        private const val MINIMUM_FREE_SPACE_AFTER_DOWNLOAD = 512L * 1024 * 1024
    }
}

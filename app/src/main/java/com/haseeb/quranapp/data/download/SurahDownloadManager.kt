package com.haseeb.quranapp.data.download

import android.content.Context
import android.util.Log
import com.haseeb.quranapp.data.local.prefs.UserPreferences
import com.haseeb.quranapp.data.remote.api.QuranApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadState(
    val progress: Float = 0f,       // 0.0 to 1.0
    val status: DownloadStatus = DownloadStatus.NOT_STARTED,
    val error: String? = null
)

enum class DownloadStatus {
    NOT_STARTED, DOWNLOADING, COMPLETED, FAILED
}

@Singleton
class SurahDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: QuranApiService,
    private val userPrefs: UserPreferences
) {
    companion object {
        private const val TAG = "SurahDownloadManager"
    }

    // Ayah counts for all 114 Surahs (used for global ID calculation)
    private val surahAyahCounts = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53,
        89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
        12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36, 25, 22, 17, 19, 26,
        30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )

    private val _downloadStates = MutableStateFlow<Map<Int, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<Int, DownloadState>> = _downloadStates.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<Int, Job>()

    // Background auto-download job
    private var autoDownloadJob: Job? = null

    init {
        // Scan for already-downloaded surahs on startup
        scope.launch {
            val states = mutableMapOf<Int, DownloadState>()
            for (surahId in 1..114) {
                if (isSurahDownloaded(surahId)) {
                    states[surahId] = DownloadState(progress = 1f, status = DownloadStatus.COMPLETED)
                }
            }
            _downloadStates.value = states
        }
    }

    fun isSurahDownloaded(surahId: Int): Boolean {
        val ayahCount = surahAyahCounts.getOrElse(surahId - 1) { 0 }
        if (ayahCount == 0) return false

        val audioDir = File(context.filesDir, "audio/arabic")
        val firstGlobalId = getGlobalAyahId(surahId, 0)

        // Check if at least the first and last Arabic audio files exist
        val firstFile = File(audioDir, "$firstGlobalId.mp3")
        val lastGlobalId = getGlobalAyahId(surahId, ayahCount - 1)
        val lastFile = File(audioDir, "$lastGlobalId.mp3")

        return firstFile.exists() && lastFile.exists()
    }

    fun downloadSurah(surahId: Int) {
        // Don't start if already downloading or completed
        val currentState = _downloadStates.value[surahId]
        if (currentState?.status == DownloadStatus.DOWNLOADING || currentState?.status == DownloadStatus.COMPLETED) return

        val job = scope.launch {
            try {
                updateState(surahId, DownloadState(progress = 0f, status = DownloadStatus.DOWNLOADING))

                val ayahCount = surahAyahCounts.getOrElse(surahId - 1) { 0 }
                if (ayahCount == 0) {
                    updateState(surahId, DownloadState(status = DownloadStatus.FAILED, error = "Invalid surah"))
                    return@launch
                }

                val reciterId = userPrefs.reciterId

                // 1. Download Arabic audio via API
                val arabicAudioUrls = try {
                    val response = api.getRecitationByChapter(reciterId, surahId)
                    response.audio_files.map { file ->
                        if (file.url.startsWith("http")) file.url
                        else if (file.url.startsWith("//")) "https:" + file.url
                        else "https://audio.qurancdn.com/" + file.url
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get audio URLs for Surah $surahId", e)
                    updateState(surahId, DownloadState(status = DownloadStatus.FAILED, error = "Failed to get audio list"))
                    return@launch
                }

                // Total items: Arabic audio + EN audio + UR audio + (4 tafseer × ayahCount) + (4 translations × ayahCount)
                val allTafsirIds = listOf(160, 159, 169, 16)
                val allTranslationIds = listOf(54, 97, 20, 85)  // Urdu Jalandhry, Urdu Tahir, EN Saheeh, EN Pickthall
                val totalItems = ayahCount * 3 + ayahCount * allTafsirIds.size + ayahCount * allTranslationIds.size
                var completed = 0

                // Download Arabic audio
                val arabicDir = File(context.filesDir, "audio/arabic")
                arabicDir.mkdirs()

                for (i in 0 until ayahCount) {
                    if (!isActive) return@launch
                    val globalId = getGlobalAyahId(surahId, i)
                    val targetFile = File(arabicDir, "$globalId.mp3")
                    if (!targetFile.exists() && i < arabicAudioUrls.size) {
                        downloadFile(arabicAudioUrls[i], targetFile)
                    }
                    completed++
                    updateState(surahId, DownloadState(progress = completed.toFloat() / totalItems, status = DownloadStatus.DOWNLOADING))
                }

                // 2. Download English translation audio
                val enDir = File(context.filesDir, "audio/en.walk")
                enDir.mkdirs()
                for (i in 0 until ayahCount) {
                    if (!isActive) return@launch
                    val globalId = getGlobalAyahId(surahId, i)
                    val targetFile = File(enDir, "$globalId.mp3")
                    if (!targetFile.exists()) {
                        val url = "https://cdn.islamic.network/quran/audio/192/en.walk/$globalId.mp3"
                        try { downloadFileWithRetry(url, targetFile) } catch (e: Exception) {
                            Log.w(TAG, "EN audio $globalId failed after retries: ${e.message}")
                        }
                    }
                    completed++
                    updateState(surahId, DownloadState(progress = completed.toFloat() / totalItems, status = DownloadStatus.DOWNLOADING))
                }

                // 3. Download Urdu translation audio
                val urDir = File(context.filesDir, "audio/ur.khan")
                urDir.mkdirs()
                for (i in 0 until ayahCount) {
                    if (!isActive) return@launch
                    val globalId = getGlobalAyahId(surahId, i)
                    val targetFile = File(urDir, "$globalId.mp3")
                    if (!targetFile.exists()) {
                        val url = "https://cdn.islamic.network/quran/audio/64/ur.khan/$globalId.mp3"
                        try { downloadFileWithRetry(url, targetFile) } catch (e: Exception) {
                            Log.w(TAG, "UR audio $globalId failed after retries: ${e.message}")
                        }
                    }
                    completed++
                    updateState(surahId, DownloadState(progress = completed.toFloat() / totalItems, status = DownloadStatus.DOWNLOADING))
                }

                // 4. Download ALL Tafseer options for each ayah
                for (tafsirId in allTafsirIds) {
                    val tafseerDir = File(context.filesDir, "tafseer/$tafsirId")
                    tafseerDir.mkdirs()
                    for (i in 0 until ayahCount) {
                        if (!isActive) return@launch
                        val verseKey = "$surahId:${i + 1}"
                        val targetFile = File(tafseerDir, "$verseKey.txt")
                        if (!targetFile.exists()) {
                            try {
                                val tafsirResponse = api.getTafsir(tafsirId, verseKey)
                                val text = tafsirResponse.tafsir?.text
                                if (text != null) {
                                    targetFile.writeText(text)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Tafseer $tafsirId/$verseKey failed: ${e.message}")
                            }
                        }
                        completed++
                        updateState(surahId, DownloadState(progress = completed.toFloat() / totalItems, status = DownloadStatus.DOWNLOADING))
                    }
                }

                // 5. Download ALL Translation texts for each ayah (for offline switching)
                for (translationId in allTranslationIds) {
                    val transDir = File(context.filesDir, "translations/$translationId")
                    transDir.mkdirs()
                    // Check if first ayah already cached for this surah
                    val firstFile = File(transDir, "$surahId:1.txt")
                    if (!firstFile.exists()) {
                        try {
                            val translationResponse = api.getTranslation(translationId)
                            val translations = translationResponse.translations ?: emptyList()
                            // The API returns ALL 6236 ayahs, we need to filter for this surah
                            val globalStart = getGlobalAyahId(surahId, 0) - 1  // 0-indexed
                            for (i in 0 until ayahCount) {
                                if (!isActive) return@launch
                                val idx = globalStart + i
                                if (idx < translations.size) {
                                    val text = translations[idx].text
                                    if (text != null) {
                                        val verseKey = "$surahId:${i + 1}"
                                        File(transDir, "$verseKey.txt").writeText(text)
                                    }
                                }
                                completed++
                                updateState(surahId, DownloadState(progress = completed.toFloat() / totalItems, status = DownloadStatus.DOWNLOADING))
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Translation $translationId for Surah $surahId failed: ${e.message}")
                            completed += ayahCount  // Skip remaining
                            updateState(surahId, DownloadState(progress = completed.toFloat() / totalItems, status = DownloadStatus.DOWNLOADING))
                        }
                    } else {
                        completed += ayahCount
                        updateState(surahId, DownloadState(progress = completed.toFloat() / totalItems, status = DownloadStatus.DOWNLOADING))
                    }
                }

                updateState(surahId, DownloadState(progress = 1f, status = DownloadStatus.COMPLETED))
                Log.d(TAG, "Surah $surahId download complete!")

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for Surah $surahId", e)
                updateState(surahId, DownloadState(status = DownloadStatus.FAILED, error = e.message))
            }
        }

        activeJobs[surahId] = job
    }

    fun cancelDownload(surahId: Int) {
        activeJobs[surahId]?.cancel()
        activeJobs.remove(surahId)
        updateState(surahId, DownloadState(status = DownloadStatus.NOT_STARTED))
    }

    /** Start auto-downloading all surahs in the background, one at a time */
    fun startAutoDownload() {
        if (autoDownloadJob?.isActive == true) return

        autoDownloadJob = scope.launch {
            for (surahId in 1..114) {
                if (!isActive) break
                val state = _downloadStates.value[surahId]
                if (state?.status != DownloadStatus.COMPLETED) {
                    downloadSurah(surahId)
                    // Wait for this download to complete before starting next
                    activeJobs[surahId]?.join()
                    // Small delay to avoid overloading
                    delay(500)
                }
            }
        }
    }

    fun stopAutoDownload() {
        autoDownloadJob?.cancel()
        autoDownloadJob = null
    }

    /** Get the local cached file path for an Arabic audio ayah, or null if not cached */
    fun getCachedArabicAudio(globalAyahId: Int): File? {
        val file = File(context.filesDir, "audio/arabic/$globalAyahId.mp3")
        return if (file.exists()) file else null
    }

    /** Get the local cached file path for a translation audio ayah, or null if not cached */
    fun getCachedTranslationAudio(langCode: String, globalAyahId: Int): File? {
        val file = File(context.filesDir, "audio/$langCode/$globalAyahId.mp3")
        return if (file.exists()) file else null
    }

    /** Get cached tafseer text, or null if not cached */
    fun getCachedTafseer(tafsirId: Int, verseKey: String): String? {
        val file = File(context.filesDir, "tafseer/$tafsirId/$verseKey.txt")
        return if (file.exists()) file.readText() else null
    }

    /** Get cached translation text, or null if not cached */
    fun getCachedTranslation(translationId: Int, verseKey: String): String? {
        val file = File(context.filesDir, "translations/$translationId/$verseKey.txt")
        return if (file.exists()) file.readText() else null
    }

    private suspend fun downloadFileWithRetry(urlStr: String, targetFile: File, maxRetries: Int = 3) {
        var lastException: Exception? = null
        for (attempt in 1..maxRetries) {
            try {
                downloadFile(urlStr, targetFile)
                return // Success
            } catch (e: Exception) {
                lastException = e
                // Delete partial file if it exists
                if (targetFile.exists()) targetFile.delete()
                if (attempt < maxRetries) {
                    val delayMs = (1000L * 2.0.pow(attempt - 1)).toLong() // 1s, 2s, 4s
                    Log.w(TAG, "Download attempt $attempt/$maxRetries failed for $urlStr, retrying in ${delayMs}ms")
                    delay(delayMs)
                }
            }
        }
        throw lastException ?: Exception("Download failed after $maxRetries attempts")
    }

    private fun downloadFile(urlStr: String, targetFile: File) {
        val url = URL(urlStr)
        val connection = url.openConnection()
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.connect()

        val inputStream = connection.getInputStream()
        val outputStream = FileOutputStream(targetFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output, bufferSize = 8192)
            }
        }
    }

    private fun getGlobalAyahId(surahId: Int, ayahIndex: Int): Int {
        var globalId = 0
        for (i in 0 until (surahId - 1)) {
            globalId += surahAyahCounts[i]
        }
        return globalId + ayahIndex + 1
    }

    private fun updateState(surahId: Int, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(surahId, state)
        }
    }
}

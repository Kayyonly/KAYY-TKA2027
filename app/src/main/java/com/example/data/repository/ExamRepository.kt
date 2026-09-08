package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ExamActiveProgress
import com.example.data.model.ExamPackage
import com.example.data.model.ExamSubmissionResult
import com.example.data.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "tka_smp_prefs")

class ExamRepository(private val context: Context) {

    private val historyKey = stringPreferencesKey("exam_history_json")
    private val activeProgressKeyPrefix = "active_progress_"

    private var cachedPackages: List<ExamPackage>? = null

    suspend fun loadPackages(): List<ExamPackage> = withContext(Dispatchers.IO) {
        cachedPackages?.let { return@withContext it }

        val packages = mutableListOf<ExamPackage>()
        try {
            val inputStream = context.assets.open("TKA_SMP_2027_complete.json")
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(jsonString)

            if (root.has("packages")) {
                val packagesArray = root.getJSONArray("packages")
                for (i in 0 until packagesArray.length()) {
                    val pkgObj = packagesArray.optJSONObject(i) ?: continue
                    val id = pkgObj.optString("id", "paket_${i + 1}")
                    val title = pkgObj.optString("title", "Paket ${i + 1}")
                    val description = pkgObj.optString("description", "")
                    val durationMinutes = pkgObj.optInt("durationMinutes", 120)

                    val questionsList = mutableListOf<Question>()
                    val questionsArray = pkgObj.optJSONArray("questions") ?: JSONArray()
                    for (j in 0 until questionsArray.length()) {
                        val qObj = questionsArray.optJSONObject(j) ?: continue
                        questionsList.add(parseQuestion(qObj, j + 1))
                    }

                    packages.add(
                        ExamPackage(
                            id = id,
                            title = title,
                            description = description,
                            durationMinutes = durationMinutes,
                            questions = questionsList
                        )
                    )
                }
            } else if (root.has("questions")) {
                val packageTitle = root.optString("title", "TKA SMP 2027")
                val notes = root.optString("notes", "")
                val questionsArray = root.getJSONArray("questions")
                val questionsList = mutableListOf<Question>()

                for (j in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(j)
                    questionsList.add(parseQuestion(qObj, j + 1))
                }

                val durationMinutes = when {
                    questionsList.size >= 100 -> 120
                    questionsList.size >= 50 -> 60
                    else -> 30
                }

                packages.add(
                    ExamPackage(
                        id = "tka_smp_2027",
                        title = packageTitle,
                        description = if (notes.isNotBlank()) notes else "Simulasi Ujian Lengkap $packageTitle (${questionsList.size} Soal).",
                        durationMinutes = durationMinutes,
                        questions = questionsList
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("ExamRepository", "Error parsing TKA_SMP_2027_complete.json: ${e.message}", e)
            throw RuntimeException("Gagal membaca atau mem-parsing TKA_SMP_2027_complete.json: ${e.message}", e)
        }

        cachedPackages = packages
        packages
    }

    private fun parseQuestion(qObj: JSONObject, defaultIndex: Int): Question {
        val qId = qObj.optInt("id", defaultIndex)
        val qText = qObj.optString("question", "").trim()
        val answerKey = if (qObj.has("answer")) {
            qObj.optString("answer", "A")
        } else {
            qObj.optString("correctAnswer", "A")
        }.trim().uppercase()

        val explanation = qObj.optString("explanation", "")
        val requiresMedia = qObj.optBoolean("requires_media", false)
        val optionsRaw = if (qObj.has("options_raw")) qObj.optString("options_raw") else null

        val contentBlocks = mutableListOf<String>()
        val cbArray = qObj.optJSONArray("content_blocks")
        if (cbArray != null) {
            for (k in 0 until cbArray.length()) {
                contentBlocks.add(cbArray.getString(k))
            }
        }

        val imagesList = mutableListOf<String>()
        val imgArray = qObj.optJSONArray("images")
        if (imgArray != null) {
            for (k in 0 until imgArray.length()) {
                val imgPath = imgArray.optString(k, "").trim()
                if (imgPath.isNotEmpty()) {
                    imagesList.add(imgPath)
                }
            }
        }

        val optionsMap = sortedMapOf<String, String>()
        val optionsObj = qObj.optJSONObject("options")
        if (optionsObj != null) {
            val keys = optionsObj.keys()
            while (keys.hasNext()) {
                val key = keys.next().trim().uppercase()
                val optVal = optionsObj.optString(key, "").trim()
                if (optVal.isNotEmpty()) {
                    optionsMap[key] = optVal
                }
            }
        }

        return Question(
            id = qId,
            question = qText,
            options = optionsMap,
            correctAnswer = answerKey,
            explanation = explanation,
            requiresMedia = requiresMedia || imagesList.isNotEmpty(),
            optionsRaw = optionsRaw,
            contentBlocks = contentBlocks,
            images = imagesList
        )
    }

    suspend fun getPackageById(packageId: String): ExamPackage? {
        val all = loadPackages()
        return all.find { it.id == packageId }
    }

    suspend fun saveExamResult(result: ExamSubmissionResult) = withContext(Dispatchers.IO) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[historyKey] ?: "[]"
            val array = try {
                JSONArray(existingJson)
            } catch (e: Exception) {
                JSONArray()
            }

            val itemObj = JSONObject().apply {
                put("packageId", result.packageId)
                put("packageTitle", result.packageTitle)
                put("totalQuestions", result.totalQuestions)
                put("correctCount", result.correctCount)
                put("wrongCount", result.wrongCount)
                put("unansweredCount", result.unansweredCount)
                put("score", result.score)
                put("durationSecondsUsed", result.durationSecondsUsed)
                put("timestamp", result.timestamp)

                val ansObj = JSONObject()
                result.userAnswers.forEach { (qId, ans) ->
                    ansObj.put(qId.toString(), ans)
                }
                put("userAnswers", ansObj)
            }

            // Put latest on top
            val newArray = JSONArray()
            newArray.put(itemObj)
            for (i in 0 until minOf(array.length(), 29)) {
                newArray.put(array.getJSONObject(i))
            }

            prefs[historyKey] = newArray.toString()
        }
    }

    fun getExamHistory(): Flow<List<ExamSubmissionResult>> {
        return context.dataStore.data.map { prefs ->
            val jsonString = prefs[historyKey] ?: "[]"
            val list = mutableListOf<ExamSubmissionResult>()
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val ansObj = obj.optJSONObject("userAnswers")
                    val answersMap = mutableMapOf<Int, String>()
                    if (ansObj != null) {
                        val keys = ansObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            answersMap[key.toInt()] = ansObj.getString(key)
                        }
                    }

                    list.add(
                        ExamSubmissionResult(
                            packageId = obj.optString("packageId"),
                            packageTitle = obj.optString("packageTitle"),
                            totalQuestions = obj.optInt("totalQuestions"),
                            correctCount = obj.optInt("correctCount"),
                            wrongCount = obj.optInt("wrongCount"),
                            unansweredCount = obj.optInt("unansweredCount"),
                            score = obj.optInt("score"),
                            durationSecondsUsed = obj.optInt("durationSecondsUsed"),
                            userAnswers = answersMap,
                            timestamp = obj.optLong("timestamp")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }
    }

    suspend fun saveActiveProgress(progress: ExamActiveProgress) = withContext(Dispatchers.IO) {
        val key = stringPreferencesKey("$activeProgressKeyPrefix${progress.packageId}")
        context.dataStore.edit { prefs ->
            val obj = JSONObject().apply {
                put("packageId", progress.packageId)
                put("currentQuestionIndex", progress.currentQuestionIndex)
                put("remainingSeconds", progress.remainingSeconds)
                put("totalQuestions", progress.totalQuestions)
                put("timestamp", progress.timestamp)
                val ansObj = JSONObject()
                progress.answers.forEach { (qId, ans) ->
                    ansObj.put(qId.toString(), ans)
                }
                put("answers", ansObj)
            }
            prefs[key] = obj.toString()
        }
    }

    suspend fun clearActiveProgress(packageId: String) = withContext(Dispatchers.IO) {
        val key = stringPreferencesKey("$activeProgressKeyPrefix$packageId")
        context.dataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    suspend fun getActiveProgress(packageId: String): ExamActiveProgress? = withContext(Dispatchers.IO) {
        val key = stringPreferencesKey("$activeProgressKeyPrefix$packageId")
        val prefs = context.dataStore.data.firstOrNull() ?: return@withContext null
        val jsonStr = prefs[key] ?: return@withContext null
        try {
            val obj = JSONObject(jsonStr)
            val pkgId = obj.optString("packageId")
            val currentIdx = obj.optInt("currentQuestionIndex", 0)
            val remainingSec = obj.optInt("remainingSeconds", 0)
            val totalQ = obj.optInt("totalQuestions", 0)
            val time = obj.optLong("timestamp", 0L)
            val ansObj = obj.optJSONObject("answers")
            val answersMap = mutableMapOf<Int, String>()
            if (ansObj != null) {
                val keys = ansObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    answersMap[k.toInt()] = ansObj.getString(k)
                }
            }
            ExamActiveProgress(
                packageId = pkgId,
                currentQuestionIndex = currentIdx,
                answers = answersMap,
                remainingSeconds = remainingSec,
                totalQuestions = totalQ,
                timestamp = time
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getAllActiveProgress(): Flow<Map<String, ExamActiveProgress>> {
        return context.dataStore.data.map { prefs ->
            val map = mutableMapOf<String, ExamActiveProgress>()
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith(activeProgressKeyPrefix) && value is String) {
                    try {
                        val obj = JSONObject(value)
                        val pkgId = obj.optString("packageId")
                        val currentIdx = obj.optInt("currentQuestionIndex", 0)
                        val remainingSec = obj.optInt("remainingSeconds", 0)
                        val totalQ = obj.optInt("totalQuestions", 0)
                        val time = obj.optLong("timestamp", 0L)
                        val ansObj = obj.optJSONObject("answers")
                        val answersMap = mutableMapOf<Int, String>()
                        if (ansObj != null) {
                            val keys = ansObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                answersMap[k.toInt()] = ansObj.getString(k)
                            }
                        }
                        if (pkgId.isNotEmpty()) {
                            map[pkgId] = ExamActiveProgress(
                                packageId = pkgId,
                                currentQuestionIndex = currentIdx,
                                answers = answersMap,
                                remainingSeconds = remainingSec,
                                totalQuestions = totalQ,
                                timestamp = time
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            map
        }
    }
}


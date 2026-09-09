package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ExamActiveProgress
import com.example.data.model.ExamPackage
import com.example.data.model.ExamSubmissionResult
import com.example.data.model.PracticeProgress
import com.example.data.model.Question
import com.example.data.model.Subject
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

    private var cachedLegacyQuestions: List<Question>? = null
    private var cachedSubjectBankQuestions: List<Question>? = null
    private var cachedPackages: List<ExamPackage>? = null

    /**
     * Source 1: LEGACY_SIMULATION
     * Loads the original 100 questions from TKA_SMP_2027_complete.json.
     * Preserves exact original order, original IDs (1-100), original options, answers, and images.
     */
    suspend fun loadLegacySimulationQuestions(): List<Question> = withContext(Dispatchers.IO) {
        cachedLegacyQuestions?.let { return@withContext it }

        val questionsList = mutableListOf<Question>()
        try {
            val inputStream = context.assets.open("TKA_SMP_2027_complete.json")
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(jsonString)

            if (root.has("questions")) {
                val questionsArray = root.getJSONArray("questions")
                for (j in 0 until questionsArray.length()) {
                    val qObj = questionsArray.getJSONObject(j)
                    questionsList.add(parseQuestion(qObj, j + 1))
                }
            } else if (root.has("packages")) {
                val packagesArray = root.getJSONArray("packages")
                for (i in 0 until packagesArray.length()) {
                    val pkgObj = packagesArray.optJSONObject(i) ?: continue
                    val questionsArray = pkgObj.optJSONArray("questions") ?: JSONArray()
                    for (j in 0 until questionsArray.length()) {
                        val qObj = questionsArray.optJSONObject(j) ?: continue
                        questionsList.add(parseQuestion(qObj, j + 1))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ExamRepository", "Error reading TKA_SMP_2027_complete.json: ${e.message}", e)
            throw RuntimeException("Gagal membaca atau mem-parsing TKA_SMP_2027_complete.json: ${e.message}", e)
        }

        cachedLegacyQuestions = questionsList
        questionsList
    }

    /**
     * Source 2: SUBJECT_BANK
     * Loads all 400 questions from TKA_SMP_2027_question_bank_400.json.
     */
    suspend fun loadAllSubjectBankQuestions(): List<Question> = withContext(Dispatchers.IO) {
        cachedSubjectBankQuestions?.let { return@withContext it }

        val questionsList = mutableListOf<Question>()
        try {
            val inputStream = context.assets.open("TKA_SMP_2027_question_bank_400.json")
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(jsonString)
            val questionsArray = root.getJSONArray("questions")
            for (j in 0 until questionsArray.length()) {
                val qObj = questionsArray.getJSONObject(j)
                questionsList.add(parseQuestion(qObj, j + 1))
            }
        } catch (e: Exception) {
            android.util.Log.e("ExamRepository", "Error reading TKA_SMP_2027_question_bank_400.json: ${e.message}", e)
            throw RuntimeException("Gagal membaca atau mem-parsing TKA_SMP_2027_question_bank_400.json: ${e.message}", e)
        }

        cachedSubjectBankQuestions = questionsList
        questionsList
    }

    /**
     * Load questions for a specific subject from the 400-question subject bank.
     * Supported subjects: "matematika", "bahasa_indonesia", "ipa", "bahasa_inggris"
     */
    suspend fun loadSubjectQuestions(subject: String): List<Question> = withContext(Dispatchers.IO) {
        val all = loadAllSubjectBankQuestions()
        val normalized = subject.trim().lowercase()
        all.filter { q ->
            val s = q.subject?.trim()?.lowercase() ?: ""
            when (normalized) {
                "matematika", "math" -> s == "matematika" || s.contains("matematika")
                "bahasa_indonesia", "indonesia" -> s == "bahasa_indonesia" || s.contains("indonesia")
                "ipa", "science" -> s == "ipa" || s.contains("ipa") || s.contains("science")
                "bahasa_inggris", "inggris", "english" -> s == "bahasa_inggris" || s.contains("inggris") || s.contains("english")
                else -> s == normalized
            }
        }
    }

    /**
     * Load questions for a specific subject and package (1..5) from the 400-question subject bank.
     * Each package contains exactly 20 questions.
     */
    suspend fun loadPackageQuestions(subject: String, packageNum: Int): List<Question> = withContext(Dispatchers.IO) {
        val subjectQuestions = loadSubjectQuestions(subject)
        val fromPackageField = subjectQuestions.filter { it.packageNum == packageNum }
        if (fromPackageField.isNotEmpty()) {
            fromPackageField
        } else {
            val chunks = subjectQuestions.chunked(20)
            if (packageNum in 1..chunks.size) chunks[packageNum - 1] else emptyList()
        }
    }

    /**
     * Total question count for a subject in the subject bank (100 questions per subject).
     */
    suspend fun getSubjectQuestionCount(subject: String): Int = withContext(Dispatchers.IO) {
        loadSubjectQuestions(subject).size
    }

    /**
     * Total package count for a subject in the subject bank (5 packages per subject).
     */
    suspend fun getPackageCount(subject: String): Int = withContext(Dispatchers.IO) {
        val total = getSubjectQuestionCount(subject)
        if (total == 0) 0 else (total + 19) / 20
    }

    /**
     * Loads all packages across both sources:
     * - LEGACY_SIMULATION: 100-question complete simulation (id: "tka_smp_2027")
     * - SUBJECT_BANK: 4 subjects x 5 packages of 20 questions = 20 packages
     */
    suspend fun loadPackages(): List<ExamPackage> = withContext(Dispatchers.IO) {
        cachedPackages?.let { return@withContext it }

        val packages = mutableListOf<ExamPackage>()

        // 1. LEGACY_SIMULATION (TKA_SMP_2027_complete.json)
        val legacyQuestions = loadLegacySimulationQuestions()
        packages.add(
            ExamPackage(
                id = "tka_smp_2027",
                title = "Simulasi TKA Lengkap",
                description = "Simulasi Ujian Lengkap 100 Soal sesuai standar resmi TKA SMP 2027.",
                durationMinutes = 120,
                questions = legacyQuestions,
                subjectId = "simulasi_lengkap"
            )
        )

        // 2. SUBJECT_BANK (TKA_SMP_2027_question_bank_400.json)
        val subjectConfigs = listOf(
            Triple("matematika", "Matematika", "Aritmetika sosial, aljabar, geometri, bangun ruang, dan statistika."),
            Triple("bahasa_indonesia", "Bahasa Indonesia", "Literasi membaca kritis, ide pokok, teks fiksi/berita, dan kaidah kebahasaan."),
            Triple("ipa", "IPA", "Fisika, Biologi, Kimia, serta metode ilmiah dan penalaran sains."),
            Triple("bahasa_inggris", "Bahasa Inggris", "Reading comprehension, functional texts, vocabulary, dan grammar.")
        )

        for ((subjId, subjName, _) in subjectConfigs) {
            val questions = loadSubjectQuestions(subjId)
            packages.addAll(generateSubjectPackages(subjId, subjName, questions))
        }

        cachedPackages = packages
        packages
    }

    private fun generateSubjectPackages(
        subjectId: String,
        subjectName: String,
        questions: List<Question>,
        packageSize: Int = 20
    ): List<ExamPackage> {
        if (questions.isEmpty()) return emptyList()

        val packageGroups = if (questions.any { it.packageNum != null }) {
            questions.groupBy { it.packageNum ?: 1 }.toSortedMap()
        } else {
            questions.chunked(packageSize).mapIndexed { idx, list -> (idx + 1) to list }.toMap()
        }

        return packageGroups.map { (pkgNum, chunk) ->
            val startQ = chunk.first().id
            val endQ = chunk.last().id
            ExamPackage(
                id = "${subjectId}_paket_$pkgNum",
                title = "Paket $pkgNum",
                description = "Latihan & Simulasi $subjectName (Soal $startQ–$endQ).",
                durationMinutes = 30, // 20 questions * 1.5 minutes
                questions = chunk,
                subjectId = subjectId
            )
        }
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

        val subject = if (qObj.has("subject")) {
            qObj.optString("subject", "")
        } else if (qObj.has("category")) {
            qObj.optString("category", "")
        } else if (qObj.has("mapel")) {
            qObj.optString("mapel", "")
        } else null

        val packageNum = if (qObj.has("package")) qObj.optInt("package") else null
        val localId = if (qObj.has("local_id")) qObj.optInt("local_id") else null
        val topic = if (qObj.has("topic")) qObj.optString("topic") else null
        val difficulty = if (qObj.has("difficulty")) qObj.optString("difficulty") else null

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
            images = imagesList,
            subject = subject,
            packageNum = packageNum,
            topic = topic,
            difficulty = difficulty,
            localId = localId
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

    suspend fun getSubjects(): List<Subject> {
        val allPkgs = loadPackages()
        return listOf(
            Subject(
                id = "matematika",
                name = "Matematika",
                description = "Aritmetika sosial, aljabar, geometri, bangun ruang, dan statistika.",
                packages = allPkgs.filter { it.subjectId == "matematika" },
                iconCategory = "math"
            ),
            Subject(
                id = "bahasa_indonesia",
                name = "Bahasa Indonesia",
                description = "Literasi membaca kritis, ide pokok, teks fiksi/berita, dan kaidah kebahasaan.",
                packages = allPkgs.filter { it.subjectId == "bahasa_indonesia" },
                iconCategory = "indonesian"
            ),
            Subject(
                id = "ipa",
                name = "IPA",
                description = "Fisika, Biologi, Kimia, serta metode ilmiah dan penalaran sains.",
                packages = allPkgs.filter { it.subjectId == "ipa" },
                iconCategory = "science"
            ),
            Subject(
                id = "bahasa_inggris",
                name = "Bahasa Inggris",
                description = "Reading comprehension, functional texts, vocabulary, dan grammar.",
                packages = allPkgs.filter { it.subjectId == "bahasa_inggris" },
                iconCategory = "english"
            ),
            Subject(
                id = "simulasi_lengkap",
                name = "Simulasi TKA Lengkap",
                description = "Tryout simulasi lengkap gabungan semua materi sesuai standar TKA SMP 2027.",
                packages = allPkgs.filter { it.id == "tka_smp_2027" },
                iconCategory = "simulation"
            )
        )
    }

    private val practiceProgressKeyPrefix = "practice_progress_"

    suspend fun savePracticeProgress(progress: PracticeProgress) = withContext(Dispatchers.IO) {
        val key = stringPreferencesKey("$practiceProgressKeyPrefix${progress.packageId}")
        context.dataStore.edit { prefs ->
            val obj = JSONObject().apply {
                put("packageId", progress.packageId)
                put("currentQuestionIndex", progress.currentQuestionIndex)
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

    suspend fun clearPracticeProgress(packageId: String) = withContext(Dispatchers.IO) {
        val key = stringPreferencesKey("$practiceProgressKeyPrefix$packageId")
        context.dataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    suspend fun getPracticeProgress(packageId: String): PracticeProgress? = withContext(Dispatchers.IO) {
        val key = stringPreferencesKey("$practiceProgressKeyPrefix$packageId")
        val prefs = context.dataStore.data.firstOrNull() ?: return@withContext null
        val jsonStr = prefs[key] ?: return@withContext null
        try {
            val obj = JSONObject(jsonStr)
            val pkgId = obj.optString("packageId")
            val currentIdx = obj.optInt("currentQuestionIndex", 0)
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
            PracticeProgress(
                packageId = pkgId,
                currentQuestionIndex = currentIdx,
                answers = answersMap,
                totalQuestions = totalQ,
                timestamp = time
            )
        } catch (e: Exception) {
            null
        }
    }

    fun getAllPracticeProgress(): Flow<Map<String, PracticeProgress>> {
        return context.dataStore.data.map { prefs ->
            val map = mutableMapOf<String, PracticeProgress>()
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith(practiceProgressKeyPrefix) && value is String) {
                    try {
                        val obj = JSONObject(value)
                        val pkgId = obj.optString("packageId")
                        val currentIdx = obj.optInt("currentQuestionIndex", 0)
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
                            map[pkgId] = PracticeProgress(
                                packageId = pkgId,
                                currentQuestionIndex = currentIdx,
                                answers = answersMap,
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


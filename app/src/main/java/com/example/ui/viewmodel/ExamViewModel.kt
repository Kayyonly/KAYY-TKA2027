package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ExamActiveProgress
import com.example.data.model.ExamMode
import com.example.data.model.ExamPackage
import com.example.data.model.ExamSubmissionResult
import com.example.data.model.PracticeProgress
import com.example.data.model.Question
import com.example.data.model.QuestionReviewItem
import com.example.data.model.Subject
import com.example.data.repository.ExamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReviewFilter {
    ALL,
    CORRECT,
    WRONG,
    UNANSWERED
}

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExamRepository(application.applicationContext)

    private val _packages = MutableStateFlow<List<ExamPackage>>(emptyList())
    val packages: StateFlow<List<ExamPackage>> = _packages.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _selectedSubject = MutableStateFlow<Subject?>(null)
    val selectedSubject: StateFlow<Subject?> = _selectedSubject.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val examHistory: StateFlow<List<ExamSubmissionResult>> = repository.getExamHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProgressMap: StateFlow<Map<String, ExamActiveProgress>> = repository.getAllActiveProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val practiceProgressMap: StateFlow<Map<String, PracticeProgress>> = repository.getAllPracticeProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Active exam state
    private val _selectedPackage = MutableStateFlow<ExamPackage?>(null)
    val selectedPackage: StateFlow<ExamPackage?> = _selectedPackage.asStateFlow()

    // Practice (Latihan) state
    private val _practicePackage = MutableStateFlow<ExamPackage?>(null)
    val practicePackage: StateFlow<ExamPackage?> = _practicePackage.asStateFlow()

    private val _practiceCurrentIndex = MutableStateFlow(0)
    val practiceCurrentIndex: StateFlow<Int> = _practiceCurrentIndex.asStateFlow()

    private val _practiceAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val practiceAnswers: StateFlow<Map<Int, String>> = _practiceAnswers.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, String>> = _userAnswers.asStateFlow()

    private val _remainingTimeSeconds = MutableStateFlow(0)
    val remainingTimeSeconds: StateFlow<Int> = _remainingTimeSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _showSubmitDialog = MutableStateFlow(false)
    val showSubmitDialog: StateFlow<Boolean> = _showSubmitDialog.asStateFlow()

    private val _showNavigatorSheet = MutableStateFlow(false)
    val showNavigatorSheet: StateFlow<Boolean> = _showNavigatorSheet.asStateFlow()

    private val _lastSubmissionResult = MutableStateFlow<ExamSubmissionResult?>(null)
    val lastSubmissionResult: StateFlow<ExamSubmissionResult?> = _lastSubmissionResult.asStateFlow()

    private val _reviewFilter = MutableStateFlow(ReviewFilter.ALL)
    val reviewFilter: StateFlow<ReviewFilter> = _reviewFilter.asStateFlow()

    private var timerJob: Job? = null
    private var totalDurationSeconds = 0

    init {
        loadPackages()
    }

    fun loadPackages() {
        viewModelScope.launch {
            _isLoading.value = true
            _packages.value = repository.loadPackages()
            _subjects.value = repository.getSubjects()
            // If a subject was selected, update reference
            val currentSelected = _selectedSubject.value
            if (currentSelected != null) {
                _selectedSubject.value = _subjects.value.find { it.id == currentSelected.id } ?: currentSelected
            }
            _isLoading.value = false
        }
    }

    fun selectSubject(subject: Subject) {
        _selectedSubject.value = subject
    }

    fun selectSubjectById(subjectId: String) {
        _selectedSubject.value = _subjects.value.find { it.id == subjectId }
    }

    fun selectPackageForInstruction(pkg: ExamPackage) {
        _selectedPackage.value = pkg
    }

    fun startExam(pkg: ExamPackage, resume: Boolean = false) {
        _selectedPackage.value = pkg
        _showSubmitDialog.value = false
        _showNavigatorSheet.value = false
        _lastSubmissionResult.value = null

        totalDurationSeconds = pkg.durationMinutes * 60

        val savedProgress = activeProgressMap.value[pkg.id]
        if (resume && savedProgress != null && savedProgress.remainingSeconds > 0) {
            val validIndex = savedProgress.currentQuestionIndex.coerceIn(0, (pkg.questions.size - 1).coerceAtLeast(0))
            _currentQuestionIndex.value = validIndex
            _userAnswers.value = savedProgress.answers
            _remainingTimeSeconds.value = savedProgress.remainingSeconds
        } else {
            _currentQuestionIndex.value = 0
            _userAnswers.value = emptyMap()
            _remainingTimeSeconds.value = totalDurationSeconds
            viewModelScope.launch {
                repository.clearActiveProgress(pkg.id)
            }
        }

        startTimer()
        persistCurrentProgress()
    }

    private fun startTimer() {
        timerJob?.cancel()
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_remainingTimeSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000L)
                val newTime = _remainingTimeSeconds.value - 1
                _remainingTimeSeconds.value = newTime

                // Periodically persist remaining time
                if (newTime % 5 == 0) {
                    persistCurrentProgress()
                }

                if (newTime <= 0) {
                    _isTimerRunning.value = false
                    submitExam()
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        persistCurrentProgress()
    }

    fun resumeTimer() {
        if (!_isTimerRunning.value && _remainingTimeSeconds.value > 0) {
            startTimer()
        }
    }

    fun selectAnswer(questionId: Int, optionKey: String) {
        val currentMap = _userAnswers.value.toMutableMap()
        if (currentMap[questionId] == optionKey) {
            // Unselect if tapped again
            currentMap.remove(questionId)
        } else {
            currentMap[questionId] = optionKey
        }
        _userAnswers.value = currentMap
        persistCurrentProgress()
    }

    fun jumpToQuestion(index: Int) {
        val total = _selectedPackage.value?.questions?.size ?: 0
        if (index in 0 until total) {
            _currentQuestionIndex.value = index
            _showNavigatorSheet.value = false
            persistCurrentProgress()
        }
    }

    fun nextQuestion() {
        val total = _selectedPackage.value?.questions?.size ?: 0
        if (_currentQuestionIndex.value < total - 1) {
            _currentQuestionIndex.value += 1
            persistCurrentProgress()
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
            persistCurrentProgress()
        }
    }

    fun openSubmitDialog() {
        _showSubmitDialog.value = true
    }

    fun closeSubmitDialog() {
        _showSubmitDialog.value = false
    }

    fun openNavigatorSheet() {
        _showNavigatorSheet.value = true
    }

    fun closeNavigatorSheet() {
        _showNavigatorSheet.value = false
    }

    private fun persistCurrentProgress() {
        val pkg = _selectedPackage.value ?: return
        val remaining = _remainingTimeSeconds.value
        val answers = _userAnswers.value
        val index = _currentQuestionIndex.value

        viewModelScope.launch {
            repository.saveActiveProgress(
                ExamActiveProgress(
                    packageId = pkg.id,
                    currentQuestionIndex = index,
                    answers = answers,
                    remainingSeconds = remaining,
                    totalQuestions = pkg.questions.size
                )
            )
        }
    }

    fun exitExamToHome() {
        pauseTimer()
        persistCurrentProgress()
        _selectedPackage.value = null
        _currentQuestionIndex.value = 0
        _userAnswers.value = emptyMap()
        _remainingTimeSeconds.value = 0
        _showSubmitDialog.value = false
        _showNavigatorSheet.value = false
    }

    fun submitExam() {
        pauseTimer()
        _showSubmitDialog.value = false
        _showNavigatorSheet.value = false

        val pkg = _selectedPackage.value ?: return
        val answers = _userAnswers.value
        val questions = pkg.questions

        var correctCount = 0
        var wrongCount = 0
        var unansweredCount = 0

        for (q in questions) {
            val userAns = answers[q.id]?.trim()?.uppercase()
            val correct = q.correctAnswer.trim().uppercase()
            when {
                userAns.isNullOrEmpty() -> unansweredCount++
                userAns == correct -> correctCount++
                else -> wrongCount++
            }
        }

        val score = if (questions.isNotEmpty()) {
            Math.round((correctCount.toDouble() / questions.size) * 100).toInt()
        } else 0

        val durationUsed = (totalDurationSeconds - _remainingTimeSeconds.value).coerceAtLeast(0)

        val result = ExamSubmissionResult(
            packageId = pkg.id,
            packageTitle = pkg.title,
            totalQuestions = questions.size,
            correctCount = correctCount,
            wrongCount = wrongCount,
            unansweredCount = unansweredCount,
            score = score,
            durationSecondsUsed = durationUsed,
            userAnswers = answers
        )

        _lastSubmissionResult.value = result

        viewModelScope.launch {
            repository.clearActiveProgress(pkg.id)
            repository.saveExamResult(result)
        }
    }

    fun setReviewFilter(filter: ReviewFilter) {
        _reviewFilter.value = filter
    }

    fun getReviewItems(): List<QuestionReviewItem> {
        val pkg = _selectedPackage.value ?: return emptyList()
        val answers = _lastSubmissionResult.value?.userAnswers ?: _userAnswers.value

        return pkg.questions.map { q ->
            val userAns = answers[q.id]?.trim()?.uppercase()
            val correctAns = q.correctAnswer.trim().uppercase()
            val isUnanswered = userAns.isNullOrEmpty()
            val isCorrect = !isUnanswered && (userAns == correctAns)

            QuestionReviewItem(
                question = q,
                userAnswer = userAns,
                isCorrect = isCorrect,
                isUnanswered = isUnanswered
            )
        }
    }

    fun resetExam() {
        pauseTimer()
        _selectedPackage.value = null
        _currentQuestionIndex.value = 0
        _userAnswers.value = emptyMap()
        _remainingTimeSeconds.value = 0
        _showSubmitDialog.value = false
        _showNavigatorSheet.value = false
        _lastSubmissionResult.value = null
    }

    // Practice (Latihan) methods
    fun startPractice(pkg: ExamPackage, resume: Boolean = true) {
        _practicePackage.value = pkg
        val saved = practiceProgressMap.value[pkg.id]
        if (resume && saved != null) {
            _practiceCurrentIndex.value = saved.currentQuestionIndex.coerceIn(0, (pkg.questions.size - 1).coerceAtLeast(0))
            _practiceAnswers.value = saved.answers
        } else {
            _practiceCurrentIndex.value = 0
            _practiceAnswers.value = emptyMap()
            viewModelScope.launch {
                repository.clearPracticeProgress(pkg.id)
            }
        }
    }

    fun answerPracticeQuestion(questionId: Int, optionKey: String) {
        val current = _practiceAnswers.value.toMutableMap()
        current[questionId] = optionKey
        _practiceAnswers.value = current
        persistPracticeProgress()
    }

    fun jumpToPracticeQuestion(index: Int) {
        val total = _practicePackage.value?.questions?.size ?: 0
        if (index in 0 until total) {
            _practiceCurrentIndex.value = index
            persistPracticeProgress()
        }
    }

    fun nextPracticeQuestion() {
        val total = _practicePackage.value?.questions?.size ?: 0
        if (_practiceCurrentIndex.value < total - 1) {
            _practiceCurrentIndex.value += 1
            persistPracticeProgress()
        }
    }

    fun previousPracticeQuestion() {
        if (_practiceCurrentIndex.value > 0) {
            _practiceCurrentIndex.value -= 1
            persistPracticeProgress()
        }
    }

    private fun persistPracticeProgress() {
        val pkg = _practicePackage.value ?: return
        val index = _practiceCurrentIndex.value
        val answers = _practiceAnswers.value
        viewModelScope.launch {
            repository.savePracticeProgress(
                PracticeProgress(
                    packageId = pkg.id,
                    currentQuestionIndex = index,
                    answers = answers,
                    totalQuestions = pkg.questions.size
                )
            )
        }
    }

    fun resetPractice(packageId: String) {
        _practiceAnswers.value = emptyMap()
        _practiceCurrentIndex.value = 0
        viewModelScope.launch {
            repository.clearPracticeProgress(packageId)
        }
    }

    fun exitPractice() {
        persistPracticeProgress()
        _practicePackage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}


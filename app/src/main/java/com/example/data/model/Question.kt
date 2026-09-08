package com.example.data.model

data class Question(
    val id: Int,
    val question: String,
    val options: Map<String, String>,
    val correctAnswer: String,
    val explanation: String = "",
    val requiresMedia: Boolean = false,
    val optionsRaw: String? = null,
    val contentBlocks: List<String> = emptyList(),
    val images: List<String> = emptyList()
)

data class ExamPackage(
    val id: String,
    val title: String,
    val description: String = "",
    val durationMinutes: Int = 30,
    val questions: List<Question> = emptyList()
)

data class ExamSubmissionResult(
    val packageId: String,
    val packageTitle: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unansweredCount: Int,
    val score: Int,
    val durationSecondsUsed: Int,
    val userAnswers: Map<Int, String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class QuestionReviewItem(
    val question: Question,
    val userAnswer: String?,
    val isCorrect: Boolean,
    val isUnanswered: Boolean
)

data class ExamActiveProgress(
    val packageId: String,
    val currentQuestionIndex: Int,
    val answers: Map<Int, String>,
    val remainingSeconds: Int,
    val totalQuestions: Int,
    val timestamp: Long = System.currentTimeMillis()
)

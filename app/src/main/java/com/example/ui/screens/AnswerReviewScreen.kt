package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Question
import com.example.ui.components.AnswerOptionCard
import com.example.ui.components.ExamProgress
import com.example.ui.components.OptionReviewStatus
import com.example.ui.components.QuestionCard
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberLight
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenLight
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedLight
import com.example.ui.viewmodel.ExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnswerReviewScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val userAnswers by viewModel.userAnswers.collectAsState()
    val submissionResult by viewModel.lastSubmissionResult.collectAsState()

    val pkg = selectedPackage ?: return
    val questions = pkg.questions
    if (questions.isEmpty()) return

    var currentReviewIndex by remember { mutableIntStateOf(0) }
    var showNavigatorSheet by remember { mutableStateOf(false) }

    val currentQuestion = questions.getOrNull(currentReviewIndex) ?: return
    val totalQuestions = questions.size
    val currentSelectedOption = userAnswers[currentQuestion.id]
    val correctOption = currentQuestion.correctAnswer.trim().uppercase()
    val isCorrect = currentSelectedOption == correctOption
    val isUnanswered = currentSelectedOption == null

    val progressFraction = if (totalQuestions > 0) (currentReviewIndex + 1).toFloat() / totalQuestions else 0f

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Review Soal ${currentReviewIndex + 1} / $totalQuestions",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Skor: ${submissionResult?.score ?: 0} / 100",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("review_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Hasil",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNavigatorSheet = true },
                            modifier = Modifier.testTag("review_open_navigator_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Daftar Soal",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                ExamProgress(
                    progress = progressFraction,
                    modifier = Modifier.testTag("review_progress_indicator")
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentReviewIndex > 0) currentReviewIndex--
                            },
                            enabled = currentReviewIndex > 0,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("review_previous_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sebelum",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = { showNavigatorSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("review_navigator_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Daftar Soal",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${currentReviewIndex + 1}/$totalQuestions",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Button(
                            onClick = {
                                if (currentReviewIndex < totalQuestions - 1) {
                                    currentReviewIndex++
                                } else {
                                    onBack()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("review_next_button")
                        ) {
                            Text(
                                text = if (currentReviewIndex < totalQuestions - 1) "Berikut" else "Selesai",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status Banner for Current Question
            val (statusText, statusBg, statusColor) = when {
                isCorrect -> Triple("Jawaban Benar", StatusGreenLight, StatusGreen)
                isUnanswered -> Triple("Tidak Dijawab (Kunci: $correctOption)", StatusAmberLight, StatusAmber)
                else -> Triple("Jawaban Salah (Pilihanmu: $currentSelectedOption • Kunci: $correctOption)", StatusRedLight, StatusRed)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusBg,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // Question Card
            AnimatedContent(
                targetState = currentQuestion,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "reviewQuestionCrossfade"
            ) { targetQuestion ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuestionCard(
                        questionNumber = currentReviewIndex + 1,
                        totalQuestions = totalQuestions,
                        questionText = targetQuestion.question,
                        requiresMedia = targetQuestion.requiresMedia,
                        optionsRaw = targetQuestion.optionsRaw,
                        images = targetQuestion.images,
                        modifier = Modifier.testTag("review_question_card_${targetQuestion.id}")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Answer Choices in Review Mode (disabled editing)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        targetQuestion.options.forEach { (key, text) ->
                            val reviewStatus = when {
                                key == correctOption -> OptionReviewStatus.CORRECT_ANSWER
                                key == currentSelectedOption && !isCorrect -> OptionReviewStatus.USER_WRONG
                                else -> OptionReviewStatus.NONE
                            }

                            AnswerOptionCard(
                                optionKey = key,
                                optionText = text,
                                isSelected = key == currentSelectedOption,
                                onSelect = {},
                                enabled = false,
                                reviewStatus = reviewStatus
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Review Question Navigator Sheet
        if (showNavigatorSheet) {
            ReviewQuestionNavigatorSheet(
                questions = questions,
                currentIndex = currentReviewIndex,
                userAnswers = userAnswers,
                onSelectQuestion = { index ->
                    currentReviewIndex = index
                    showNavigatorSheet = false
                },
                onDismiss = { showNavigatorSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewQuestionNavigatorSheet(
    questions: List<Question>,
    currentIndex: Int,
    userAnswers: Map<Int, String>,
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Status Jawaban Soal",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ReviewLegendDot(color = StatusGreen, label = "Benar")
                ReviewLegendDot(color = StatusRed, label = "Salah")
                ReviewLegendDot(color = StatusAmber, label = "Kosong")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                itemsIndexed(questions) { index, question ->
                    val isCurrent = index == currentIndex
                    val userAns = userAnswers[question.id]
                    val correctAns = question.correctAnswer.trim().uppercase()
                    val isQCorrect = userAns == correctAns
                    val isQUnanswered = userAns == null

                    val (bgColor, borderColor, textColor) = when {
                        isQCorrect -> Triple(StatusGreenLight, StatusGreen, StatusGreen)
                        isQUnanswered -> Triple(StatusAmberLight, StatusAmber, StatusAmber)
                        else -> Triple(StatusRedLight, StatusRed, StatusRed)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bgColor,
                        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else borderColor),
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectQuestion(index) }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                            )
                            Text(
                                text = userAns ?: "-",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewLegendDot(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = color,
            modifier = Modifier.size(12.dp)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

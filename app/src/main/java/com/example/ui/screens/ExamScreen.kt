package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AnswerOptionCard
import com.example.ui.components.ExamProgress
import com.example.ui.components.QuestionCard
import com.example.ui.components.TimerChip
import com.example.ui.viewmodel.ExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamScreen(
    viewModel: ExamViewModel,
    onExamFinished: () -> Unit,
    onExitExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val userAnswers by viewModel.userAnswers.collectAsState()
    val remainingTime by viewModel.remainingTimeSeconds.collectAsState()
    val showSubmitDialog by viewModel.showSubmitDialog.collectAsState()
    val showNavigatorSheet by viewModel.showNavigatorSheet.collectAsState()

    val pkg = selectedPackage ?: return
    val questions = pkg.questions
    if (questions.isEmpty()) return

    val currentQuestion = questions.getOrNull(currentIndex) ?: return
    val totalQuestions = questions.size
    val answeredCount = questions.count { userAnswers.containsKey(it.id) }
    val unansweredCount = totalQuestions - answeredCount
    val currentSelectedOption = userAnswers[currentQuestion.id]
    val progressFraction = if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions else 0f
    val isLastQuestion = currentIndex == totalQuestions - 1

    BackHandler {
        viewModel.openSubmitDialog()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Soal ${currentIndex + 1} / $totalQuestions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.openSubmitDialog() },
                            modifier = Modifier.testTag("exam_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali / Kumpulkan",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        TimerChip(remainingSeconds = remainingTime)
                        Spacer(modifier = Modifier.width(12.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                ExamProgress(
                    progress = progressFraction,
                    modifier = Modifier.testTag("exam_progress_indicator")
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
                        // Previous Button
                        OutlinedButton(
                            onClick = { viewModel.previousQuestion() },
                            enabled = currentIndex > 0,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("previous_question_button")
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

                        // Question Navigator Button (Center)
                        OutlinedButton(
                            onClick = { viewModel.openNavigatorSheet() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("quick_navigator_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Daftar Soal",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$answeredCount/$totalQuestions",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        // Next or Finish Button
                        if (!isLastQuestion) {
                            Button(
                                onClick = { viewModel.nextQuestion() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("next_question_button")
                            ) {
                                Text(
                                    text = "Berikut",
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
                        } else {
                            // On last question, replace Next with Finish button
                            Button(
                                onClick = { viewModel.openSubmitDialog() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("finish_exam_inline_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Selesai",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated Question Switch
            AnimatedContent(
                targetState = currentQuestion,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "questionCrossfade"
            ) { targetQuestion ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Spacious Question Card
                    QuestionCard(
                        questionNumber = currentIndex + 1,
                        totalQuestions = totalQuestions,
                        questionText = targetQuestion.question,
                        requiresMedia = targetQuestion.requiresMedia,
                        optionsRaw = targetQuestion.optionsRaw,
                        images = targetQuestion.images,
                        modifier = Modifier.testTag("question_card_${targetQuestion.id}")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Answer Choice Cards (A, B, C, D, E)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        targetQuestion.options.forEach { (key, text) ->
                            AnswerOptionCard(
                                optionKey = key,
                                optionText = text,
                                isSelected = currentSelectedOption == key,
                                onSelect = {
                                    viewModel.selectAnswer(targetQuestion.id, key)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Question Navigator Bottom Sheet
        if (showNavigatorSheet) {
            QuestionNavigatorSheet(
                questions = questions,
                currentIndex = currentIndex,
                userAnswers = userAnswers,
                onSelectQuestion = { selectedIndex ->
                    viewModel.jumpToQuestion(selectedIndex)
                },
                onDismiss = {
                    viewModel.closeNavigatorSheet()
                }
            )
        }

        // Submit Confirmation Dialog
        if (showSubmitDialog) {
            SubmitConfirmationDialog(
                totalQuestions = totalQuestions,
                answeredCount = answeredCount,
                unansweredCount = unansweredCount,
                onConfirm = {
                    viewModel.submitExam()
                    onExamFinished()
                },
                onDismiss = {
                    viewModel.closeSubmitDialog()
                },
                onSaveAndExit = {
                    viewModel.exitExamToHome()
                    onExitExam()
                }
            )
        }
    }
}

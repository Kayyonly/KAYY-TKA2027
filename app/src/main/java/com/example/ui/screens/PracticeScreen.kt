package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Question
import com.example.ui.components.AnswerOptionCard
import com.example.ui.components.OptionReviewStatus
import com.example.ui.components.QuestionCard
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenLight
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedLight
import com.example.ui.viewmodel.ExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pkg by viewModel.practicePackage.collectAsState()
    val currentIndex by viewModel.practiceCurrentIndex.collectAsState()
    val practiceAnswers by viewModel.practiceAnswers.collectAsState()

    var showNavigatorSheet by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    val currentPkg = pkg ?: return
    val questions = currentPkg.questions
    if (questions.isEmpty()) return

    val currentQuestion = questions.getOrNull(currentIndex) ?: return
    val totalQuestions = questions.size
    val answeredCount = questions.count { practiceAnswers.containsKey(it.id) }
    val userAnswer = practiceAnswers[currentQuestion.id]
    val isAnswered = userAnswer != null
    val progressFraction = if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions else 0f
    val isLastQuestion = currentIndex == totalQuestions - 1

    BackHandler {
        viewModel.exitPractice()
        onBack()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Latihan",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${currentIndex + 1} / $totalQuestions",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentPkg.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.exitPractice()
                                onBack()
                            },
                            modifier = Modifier.testTag("practice_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Materi"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNavigatorSheet = true },
                            modifier = Modifier.testTag("practice_navigator_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Daftar Soal",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        OutlinedButton(
                            onClick = { viewModel.previousPracticeQuestion() },
                            enabled = currentIndex > 0,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("practice_btn_previous")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sebelumnya",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Next or Finish Button
                        if (isLastQuestion) {
                            Button(
                                onClick = { showCompletionDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("practice_btn_finish")
                            ) {
                                Text(
                                    text = "Selesai",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.nextPracticeQuestion() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("practice_btn_next")
                            ) {
                                Text(
                                    text = "Selanjutnya",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
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
            AnimatedContent(
                targetState = currentQuestion,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "practiceQuestionSwitch"
            ) { targetQuestion ->
                val currentTargetAns = practiceAnswers[targetQuestion.id]
                val isCurrentTargetAnswered = currentTargetAns != null
                val normalizedCorrect = targetQuestion.correctAnswer.trim().uppercase()

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Question Card
                    QuestionCard(
                        questionNumber = currentIndex + 1,
                        totalQuestions = totalQuestions,
                        questionText = targetQuestion.question,
                        requiresMedia = targetQuestion.requiresMedia,
                        optionsRaw = targetQuestion.optionsRaw,
                        images = targetQuestion.images,
                        modifier = Modifier.testTag("practice_question_card_${targetQuestion.id}")
                    )

                    // Instruction / Hint Banner before answering
                    if (!isCurrentTargetAnswered) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Pilih salah satu jawaban di bawah. Kunci jawaban dan pembahasan akan langsung terbuka.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    // Answer Choice Cards
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        targetQuestion.options.forEach { (key, text) ->
                            val isSelected = currentTargetAns == key
                            val reviewStatus = when {
                                !isCurrentTargetAnswered -> OptionReviewStatus.NONE
                                key == normalizedCorrect -> OptionReviewStatus.CORRECT_ANSWER
                                isSelected && key != normalizedCorrect -> OptionReviewStatus.USER_WRONG
                                else -> OptionReviewStatus.NONE
                            }

                            AnswerOptionCard(
                                optionKey = key,
                                optionText = text,
                                isSelected = isSelected,
                                reviewStatus = reviewStatus,
                                enabled = true, // Student can tap to change/test answers
                                onSelect = {
                                    viewModel.answerPracticeQuestion(targetQuestion.id, key)
                                }
                            )
                        }
                    }

                    // Immediate Feedback & Structured Explanation Card
                    if (isCurrentTargetAnswered) {
                        val isCorrect = currentTargetAns.trim().uppercase() == normalizedCorrect

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) StatusGreenLight else StatusRedLight
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isCorrect) StatusGreen.copy(alpha = 0.5f) else StatusRed.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("practice_feedback_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isCorrect) StatusGreen else StatusRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = if (isCorrect) "Jawaban Kamu Benar!" else "Jawaban Kamu Kurang Tepat",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCorrect) StatusGreen else StatusRed
                                        )
                                    )
                                }

                                if (!isCorrect) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Kunci Jawaban yang Benar: $normalizedCorrect",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.padding(start = 38.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(
                                    color = if (isCorrect) StatusGreen.copy(alpha = 0.25f) else StatusRed.copy(alpha = 0.25f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Explanation Section (structured for future rich explanations)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pembahasan & Konsep Soal",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val explanationText = if (targetQuestion.explanation.isNotBlank()) {
                                    targetQuestion.explanation
                                } else {
                                    "Kunci jawaban yang tepat adalah opsi $normalizedCorrect.\nPembahasan terperinci langkah penyelesaian soal ini akan diperkaya pada update modul berikutnya."
                                }

                                Text(
                                    text = explanationText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 22.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Question Navigator Bottom Sheet
        if (showNavigatorSheet) {
            PracticeNavigatorBottomSheet(
                questions = questions,
                currentIndex = currentIndex,
                practiceAnswers = practiceAnswers,
                onSelectQuestion = { selectedIndex ->
                    viewModel.jumpToPracticeQuestion(selectedIndex)
                    showNavigatorSheet = false
                },
                onDismiss = { showNavigatorSheet = false }
            )
        }

        // Completion Dialog
        if (showCompletionDialog) {
            var correctCount = 0
            var wrongCount = 0
            questions.forEach { q ->
                val ans = practiceAnswers[q.id]?.trim()?.uppercase()
                val correct = q.correctAnswer.trim().uppercase()
                if (ans != null) {
                    if (ans == correct) correctCount++ else wrongCount++
                }
            }
            val unansweredCount = totalQuestions - (correctCount + wrongCount)
            val scorePercent = if (totalQuestions > 0) ((correctCount.toFloat() / totalQuestions) * 100).toInt() else 0

            Dialog(onDismissRequest = { showCompletionDialog = false }) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Latihan Selesai!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Kamu telah menyelesaikan seluruh soal pada paket ${currentPkg.title}.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Stats Summary Row
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$correctCount",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StatusGreen
                                    )
                                )
                                Text("Benar", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$wrongCount",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = StatusRed
                                    )
                                )
                                Text("Salah", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$unansweredCount",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text("Kosong", style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$scorePercent%",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text("Akurasi", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = {
                                showCompletionDialog = false
                                viewModel.exitPractice()
                                onBack()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_close_practice_summary")
                        ) {
                            Text("Kembali ke Modul Materi")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                showCompletionDialog = false
                                viewModel.resetPractice(currentPkg.id)
                                viewModel.startPractice(currentPkg, resume = false)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_restart_practice_summary")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ulangi Latihan dari Awal")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet for jumping between questions in Practice mode.
 * Shows color coding: Green for answered correctly, Red for wrong, Gray for unanswered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeNavigatorBottomSheet(
    questions: List<Question>,
    currentIndex: Int,
    practiceAnswers: Map<Int, String>,
    onSelectQuestion: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Daftar Nomor Soal Latihan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${practiceAnswers.size} / ${questions.size} Terjawab",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(StatusGreen))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Benar", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(StatusRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salah", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Belum Dijawab", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                itemsIndexed(questions) { index, question ->
                    val userAns = practiceAnswers[question.id]?.trim()?.uppercase()
                    val correctAns = question.correctAnswer.trim().uppercase()
                    val isCurrent = index == currentIndex

                    val containerColor = when {
                        isCurrent -> MaterialTheme.colorScheme.primaryContainer
                        userAns == null -> MaterialTheme.colorScheme.surfaceVariant
                        userAns == correctAns -> StatusGreenLight
                        else -> StatusRedLight
                    }

                    val textColor = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimaryContainer
                        userAns == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        userAns == correctAns -> StatusGreen
                        else -> StatusRed
                    }

                    val borderColor = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = containerColor,
                        border = BorderStroke(if (isCurrent) 2.dp else 0.dp, borderColor),
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectQuestion(index) }
                            .testTag("practice_jump_question_$index")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

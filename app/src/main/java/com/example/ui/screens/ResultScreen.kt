package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppTopBar
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ResultStatCard
import com.example.ui.components.SecondaryButton
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ExamViewModel

@Composable
fun ResultScreen(
    viewModel: ExamViewModel,
    onReviewAnswers: () -> Unit,
    onRetakeExam: () -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val submissionResult by viewModel.lastSubmissionResult.collectAsState()
    val selectedPackage by viewModel.selectedPackage.collectAsState()

    val result = submissionResult ?: return
    val pkg = selectedPackage

    val minutesUsed = result.durationSecondsUsed / 60
    val secondsUsed = result.durationSecondsUsed % 60
    val timeUsedFormatted = String.format("%02d:%02d", minutesUsed, secondsUsed)

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Hasil Ujian",
                subtitle = result.packageTitle
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clean Score Summary Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_score_card")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "SKOR AKHIR",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${result.score}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Text(
                        text = "dari 100",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Waktu Pengerjaan",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "$timeUsedFormatted Menit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Simple Stat Cards (Correct, Wrong, Unanswered)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ResultStatCard(
                    title = "Benar",
                    value = "${result.correctCount}",
                    color = StatusGreen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_correct_count")
                )
                ResultStatCard(
                    title = "Salah",
                    value = "${result.wrongCount}",
                    color = StatusRed,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_wrong_count")
                )
                ResultStatCard(
                    title = "Kosong",
                    value = "${result.unansweredCount}",
                    color = StatusAmber,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stat_unanswered_count")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            PrimaryButton(
                text = "Review Pembahasan",
                onClick = onReviewAnswers,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.testTag("result_review_answers_button")
            )

            if (pkg != null) {
                SecondaryButton(
                    text = "Ulangi Ujian Ini",
                    onClick = {
                        viewModel.startExam(pkg)
                        onRetakeExam()
                    },
                    leadingIcon = Icons.Default.Refresh,
                    modifier = Modifier.testTag("result_retake_button")
                )
            }

            SecondaryButton(
                text = "Kembali ke Beranda",
                onClick = {
                    viewModel.resetExam()
                    onBackHome()
                },
                leadingIcon = Icons.Default.Home,
                modifier = Modifier.testTag("result_back_home_button")
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

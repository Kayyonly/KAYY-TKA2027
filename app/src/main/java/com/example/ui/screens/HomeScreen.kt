package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamPackage
import com.example.ui.components.ExamPackageCard
import com.example.ui.components.PrimaryButton
import com.example.ui.components.ResumeExamBanner
import com.example.ui.viewmodel.ExamViewModel

@Composable
fun HomeScreen(
    viewModel: ExamViewModel,
    onNavigateToPackages: () -> Unit,
    onSelectPackage: (ExamPackage) -> Unit,
    onResumeExam: (ExamPackage) -> Unit,
    modifier: Modifier = Modifier
) {
    val packages by viewModel.packages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val examHistory by viewModel.examHistory.collectAsState()
    val activeProgressMap by viewModel.activeProgressMap.collectAsState()

    // Find if there is an active unfinished package
    val activeEntry = activeProgressMap.entries.firstOrNull { (_, progress) ->
        progress.remainingSeconds > 0 && packages.any { it.id == progress.packageId }
    }
    val activePackage = activeEntry?.let { entry ->
        packages.firstOrNull { it.id == entry.key }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Minimal Header Section
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Column {
                        Text(
                            text = "TKA SMP 2027",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latihan TKA SMP • Simulasi Ujian Mandiri",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Resume Exam Banner if user has an unfinished exam
                if (activePackage != null && activeEntry != null) {
                    val progress = activeEntry.value
                    item {
                        ResumeExamBanner(
                            pkg = activePackage,
                            progress = progress,
                            onResume = { onResumeExam(activePackage) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                } else if (packages.isNotEmpty()) {
                    // Primary CTA Card (Focus CTA) if no active exam
                    item {
                        val firstPackage = packages.first()
                        PrimaryButton(
                            text = "Mulai Latihan (${firstPackage.title})",
                            onClick = { onSelectPackage(firstPackage) },
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                            modifier = Modifier.testTag("home_primary_cta")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                // Section Title
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Paket Ujian Tersedia",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "${packages.size} Paket",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Package List
                items(packages) { pkg ->
                    val lastScore = examHistory.firstOrNull { it.packageId == pkg.id }?.score
                    val activeProgress = activeProgressMap[pkg.id]
                    ExamPackageCard(
                        pkg = pkg,
                        onSelect = {
                            if (activeProgress != null && activeProgress.remainingSeconds > 0) {
                                onResumeExam(pkg)
                            } else {
                                onSelectPackage(pkg)
                            }
                        },
                        lastScore = lastScore,
                        activeProgress = activeProgress
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}


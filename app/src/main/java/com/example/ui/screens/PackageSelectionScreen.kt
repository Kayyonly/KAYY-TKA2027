package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamPackage
import com.example.ui.components.AppTopBar
import com.example.ui.components.ExamPackageCard
import com.example.ui.viewmodel.ExamViewModel

@Composable
fun PackageSelectionScreen(
    viewModel: ExamViewModel,
    onBack: () -> Unit,
    onSelectPackage: (ExamPackage) -> Unit,
    modifier: Modifier = Modifier
) {
    val packages by viewModel.packages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val examHistory by viewModel.examHistory.collectAsState()
    val activeProgressMap by viewModel.activeProgressMap.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Pilih Paket Soal",
                subtitle = "Pilih modul latihan yang ingin kamu kerjakan",
                onBack = onBack
            )
        },
        modifier = modifier.fillMaxSize()
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                items(packages) { pkg ->
                    val lastScore = examHistory.firstOrNull { it.packageId == pkg.id }?.score
                    val activeProgress = activeProgressMap[pkg.id]
                    ExamPackageCard(
                        pkg = pkg,
                        onSelect = { onSelectPackage(pkg) },
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

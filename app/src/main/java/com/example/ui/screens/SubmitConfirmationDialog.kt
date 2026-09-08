package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberLight
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed

@Composable
fun SubmitConfirmationDialog(
    totalQuestions: Int,
    answeredCount: Int,
    unansweredCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSaveAndExit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hasUnanswered = unansweredCount > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Kumpulkan Lembar Ujian?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (hasUnanswered) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusAmberLight,
                        border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Terdapat $unansweredCount soal yang belum dijawab. Apakah kamu tetap ingin mengumpulkan sekarang?",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = StatusAmber,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Semua soal telah kamu jawab. Apakah kamu siap untuk menyelesaikan ujian dan melihat hasil penilaian?",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    )
                }

                // Summary Stats
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DialogStatRow(
                            label = "Total Soal",
                            value = "$totalQuestions Soal",
                            valueColor = MaterialTheme.colorScheme.onSurface
                        )
                        DialogStatRow(
                            label = "Sudah Dijawab",
                            value = "$answeredCount Soal",
                            valueColor = StatusGreen
                        )
                        DialogStatRow(
                            label = "Belum Dijawab",
                            value = "$unansweredCount Soal",
                            valueColor = if (hasUnanswered) StatusRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onSaveAndExit != null) {
                    OutlinedButton(
                        onClick = onSaveAndExit,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_save_exit_button")
                    ) {
                        Text(
                            text = "Simpan & Lanjutkan Nanti",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("dialog_confirm_submit_button")
            ) {
                Text(
                    text = "Kumpulkan",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.testTag("dialog_cancel_submit_button")
            ) {
                Text(
                    text = "Periksa Lagi",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        modifier = modifier.testTag("submit_confirmation_dialog")
    )
}


@Composable
private fun DialogStatRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}

package com.sevenpro.management.ui.screens.reports

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.model.*
import com.sevenpro.management.data.repository.FinancialRepository
import com.sevenpro.management.data.repository.EarningsRepository
import com.sevenpro.management.data.repository.TeacherPaymentRepository
import com.sevenpro.management.export.CsvExporter
import com.sevenpro.management.export.ExcelExporter
import com.sevenpro.management.export.PdfExporter
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    supabaseClient: SupabaseClient,
    isMainAdmin: Boolean
) {
    val financialRepo = remember { FinancialRepository(supabaseClient) }
    val earningsRepo = remember { EarningsRepository(supabaseClient) }
    val teacherPaymentRepo = remember { TeacherPaymentRepository(supabaseClient) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var report by remember { mutableStateOf<FinancialReport?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableIntStateOf(0) }

    val today = LocalDate.now()
    var startDate by remember { mutableStateOf(today.withDayOfMonth(1).toString()) }
    var endDate by remember { mutableStateOf(today.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Reports & Export", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Date Range
            Text("Date Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("From") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("To") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Quick Period Selection
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "This Month" to Pair(today.withDayOfMonth(1).toString(), today.toString()),
                    "Last Month" to Pair(today.minusMonths(1).withDayOfMonth(1).toString(), today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth()).toString()),
                    "This Quarter" to Pair(today.withMonth(((today.monthValue - 1) / 3) * 3 + 1).withDayOfMonth(1).toString(), today.toString()),
                    "This Year" to Pair(today.withDayOfYear(1).toString(), today.toString())
                ).forEach { (label, dates) ->
                    AssistChip(
                        onClick = { startDate = dates.first; endDate = dates.second },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        report = financialRepo.getFinancialReport(startDate, endDate)
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Assessment, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Report")
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            report?.let { r ->
                Spacer(modifier = Modifier.height(16.dp))

                // Report Summary
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Financial Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${r.periodStart} to ${r.periodEnd}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        ReportRow("Total Revenue", "$${String.format("%.2f", r.totalRevenue)}", MaterialTheme.colorScheme.tertiary)
                        ReportRow("Total Expenses", "-$${String.format("%.2f", r.totalExpenses)}", MaterialTheme.colorScheme.error)
                        ReportRow("Teacher Earnings", "-$${String.format("%.2f", r.teacherEarnings)}", MaterialTheme.colorScheme.warning)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ReportRow("Net Profit", "$${String.format("%.2f", r.netProfit)}",
                            if (r.netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            isBold = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ReportRow("Subscriptions Count", "${r.subscriptionCount}", MaterialTheme.colorScheme.primary)
                    }
                }

                // Category Breakdown
                if (r.paymentBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Category Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            r.paymentBreakdown.forEach { (cat, amount) ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(cat.ifEmpty { "Uncategorized" }, style = MaterialTheme.typography.bodyMedium)
                                    Text("$${String.format("%.2f", amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Export Buttons
                Spacer(modifier = Modifier.height(16.dp))
                Text("Export Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportButton(
                        label = "PDF",
                        icon = Icons.Filled.PictureAsPdf,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { exportReport(context, r, ExportFormat.PDF) }
                    )
                    ExportButton(
                        label = "CSV",
                        icon = Icons.Filled.TableChart,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { exportReport(context, r, ExportFormat.CSV) }
                    )
                    ExportButton(
                        label = "Excel",
                        icon = Icons.Filled.Sheets,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { exportReport(context, r, ExportFormat.EXCEL) }
                    )
                }

                // Schedule teacher payments
                if (isMainAdmin) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                teacherPaymentRepo.scheduleTeacherPayments()
                                Toast.makeText(context, "Teacher payments scheduled", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.CalendarMonth, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Schedule Teacher Payments")
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, color: Color, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun ExportButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}

private fun exportReport(context: Context, report: FinancialReport, format: ExportFormat) {
    try {
        val file = when (format) {
            ExportFormat.PDF -> PdfExporter.exportFinancialReport(context, report)
            ExportFormat.CSV -> CsvExporter.exportFinancialReport(context, report)
            ExportFormat.EXCEL -> ExcelExporter.exportFinancialReport(context, report)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", file
                ),
                when (format) {
                    ExportFormat.PDF -> "application/pdf"
                    ExportFormat.CSV -> "text/csv"
                    ExportFormat.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                }
            )
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Open report"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

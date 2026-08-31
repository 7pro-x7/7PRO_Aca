package com.sevenpro.management.ui.screens.earnings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.local.UserPreferences
import com.sevenpro.management.data.model.TeacherPayment
import com.sevenpro.management.data.repository.TeacherPaymentRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPaymentScreen(
    supabaseClient: SupabaseClient,
    userPreferences: UserPreferences
) {
    val paymentRepo = remember { TeacherPaymentRepository(supabaseClient) }
    val scope = rememberCoroutineScope()

    val userId by userPreferences.userId.collectAsState(initial = null)

    var payments by remember { mutableStateOf<List<TeacherPayment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(userId, selectedTab) {
        userId?.let { uid ->
            scope.launch {
                payments = paymentRepo.getTeacherPayments(uid)
                isLoading = false
            }
        }
    }

    val filteredPayments = when (selectedTab) {
        0 -> payments // All
        1 -> payments.filter { it.status == "pending" }
        2 -> payments.filter { it.status == "paid" }
        else -> payments
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Payments", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                // Summary
                val totalPending = payments.filter { it.status == "pending" }.sumOf { it.total_amount }
                val totalPaid = payments.filter { it.status == "paid" }.sumOf { it.total_amount }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$${String.format("%.0f", totalPending)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.warning)
                            Text("Pending", style = MaterialTheme.typography.labelMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$${String.format("%.0f", totalPaid)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            Text("Paid", style = MaterialTheme.typography.labelMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${payments.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Total", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("All") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Pending") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Paid") })
                }

                if (filteredPayments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No payments found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredPayments) { payment ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (payment.status == "paid") Icons.Filled.CheckCircle else Icons.Filled.Schedule,
                                        null,
                                        tint = if (payment.status == "paid") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.warning,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${payment.period_start} - ${payment.period_end}", fontWeight = FontWeight.Medium)
                                        Text(
                                            if (payment.status == "paid") "Paid on ${payment.paid_date}" else "Scheduled payment",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "$${String.format("%.2f", payment.total_amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (payment.status == "paid") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

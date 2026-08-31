package com.sevenpro.management.ui.screens.payments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.model.Payment
import com.sevenpro.management.data.repository.PaymentRepository
import com.sevenpro.management.ui.screens.subscriptions.StatusBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentListScreen(
    supabaseClient: SupabaseClient,
    isAdmin: Boolean,
    onPaymentClick: (String) -> Unit
) {
    val paymentRepo = remember { PaymentRepository(supabaseClient) }
    val scope = rememberCoroutineScope()

    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatus by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    val statuses = listOf("all" to "All", "pending" to "Pending", "paid" to "Paid", "overdue" to "Overdue")

    LaunchedEffect(selectedStatus) {
        isLoading = true
        scope.launch {
            try {
                payments = when (selectedStatus) {
                    "overdue" -> paymentRepo.getOverduePayments()
                    "pending", "paid" -> paymentRepo.getPaymentsByStatus(selectedStatus)
                    else -> paymentRepo.getAllPayments()
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    val filtered = if (searchQuery.isBlank()) payments
    else payments.filter {
        it.notes.contains(searchQuery, ignoreCase = true) ||
                it.reference_number?.contains(searchQuery, ignoreCase = true) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Payments", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search payments...") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status Filters
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                statuses.forEach { (status, label) ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Payments, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No payments found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${payments.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Total", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$${String.format("%.0f", payments.sumOf { it.amount })}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Amount", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${payments.count { it.status == "pending" }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.warning)
                            Text("Pending", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { payment ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onPaymentClick(payment.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = if (payment.status == "overdue") CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                            else CardDefaults.cardColors()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        payment.notes.ifEmpty { "Payment" },
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text("Due: ${payment.due_date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$${String.format("%.2f", payment.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    StatusBadge(payment.status)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

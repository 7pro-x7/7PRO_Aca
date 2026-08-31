package com.sevenpro.management.ui.screens.payments

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.model.Payment
import com.sevenpro.management.data.repository.PaymentRepository
import com.sevenpro.management.data.repository.EarningsRepository
import com.sevenpro.management.ui.screens.users.InfoRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    supabaseClient: SupabaseClient,
    paymentId: String,
    onBack: () -> Unit
) {
    val paymentRepo = remember { PaymentRepository(supabaseClient) }
    val earningRepo = remember { EarningsRepository(supabaseClient) }
    val scope = rememberCoroutineScope()

    var payment by remember { mutableStateOf<Payment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showPaidDialog by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("cash") }

    LaunchedEffect(paymentId) {
        scope.launch {
            payment = paymentRepo.getAllPayments().find { it.id == paymentId }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (payment == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Payment not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Amount Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$${String.format("%.2f", payment!!.amount)}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (payment!!.status) {
                                "paid" -> MaterialTheme.colorScheme.tertiary
                                "pending" -> MaterialTheme.colorScheme.warning
                                "overdue" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                payment!!.status.uppercase(),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Payment Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow("Payment ID", payment!!.id.take(12) + "...")
                        InfoRow("Due Date", payment!!.due_date)
                        InfoRow("Paid Date", payment!!.paid_date ?: "Not paid yet")
                        InfoRow("Method", payment!!.payment_method ?: "Not specified")
                        InfoRow("Reference", payment!!.reference_number ?: "None")
                        InfoRow("Notes", payment!!.notes.ifEmpty { "None" })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                if (payment!!.status == "pending" || payment!!.status == "overdue") {
                    Button(
                        onClick = { showPaidDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mark as Paid", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        if (showPaidDialog) {
            AlertDialog(
                onDismissRequest = { showPaidDialog = false },
                title = { Text("Mark as Paid") },
                text = {
                    Column {
                        Text("Select payment method:")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("cash" to "Cash", "card" to "Card", "transfer" to "Transfer", "online" to "Online").forEach { (method, label) ->
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { paymentMethod = method },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            payment?.let {
                                paymentRepo.markAsPaid(it.id, paymentMethod)
                                // Calculate teacher earnings
                                earningRepo.calculateEarningsForPayment(it)
                                payment = it.copy(
                                    status = "paid",
                                    paid_date = java.time.LocalDate.now().toString(),
                                    payment_method = paymentMethod
                                )
                            }
                            showPaidDialog = false
                        }
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPaidDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

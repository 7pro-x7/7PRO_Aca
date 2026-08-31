package com.sevenpro.management.ui.screens.earnings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.local.UserPreferences
import com.sevenpro.management.data.model.TeacherEarning
import com.sevenpro.management.data.model.TeacherProfile
import com.sevenpro.management.data.model.UserProfile
import com.sevenpro.management.data.repository.TeacherRepository
import com.sevenpro.management.data.repository.EarningsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherEarningsScreen(
    supabaseClient: SupabaseClient,
    userPreferences: UserPreferences,
    isAdmin: Boolean,
    isMainAdmin: Boolean
) {
    val teacherRepo = remember { TeacherRepository(supabaseClient) }
    val earningRepo = remember { EarningsRepository(supabaseClient) }
    val scope = rememberCoroutineScope()

    val userRole by userPreferences.userRole.collectAsState(initial = null)
    val userId by userPreferences.userId.collectAsState(initial = null)

    var teachers by remember { mutableStateOf<List<Pair<TeacherProfile, UserProfile>>>(emptyList()) }
    var selectedTeacherId by remember { mutableStateOf<String?>(null) }
    var earnings by remember { mutableStateOf<List<TeacherEarning>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditPercentageDialog by remember { mutableStateOf(false) }
    var newPercentage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            if (isAdmin) {
                teachers = teacherRepo.getTeachersWithUsers()
                if (teachers.isNotEmpty() && selectedTeacherId == null) {
                    selectedTeacherId = teachers.first().first.user_id
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedTeacherId) {
        selectedTeacherId?.let { tid ->
            scope.launch {
                earnings = earningRepo.getTeacherEarnings(tid)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Teacher Earnings", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Teacher selector (admin only)
                if (isAdmin && teachers.isNotEmpty()) {
                    item {
                        Text("Select Teacher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Teacher chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            teachers.take(4).forEach { (teacher, user) ->
                                FilterChip(
                                    selected = selectedTeacherId == teacher.user_id,
                                    onClick = { selectedTeacherId = teacher.user_id },
                                    label = { Text(user.full_name.split(" ").first(), style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // Earnings Summary
                val totalEarned = earnings.sumOf { it.earned_amount }
                val pendingEarnings = earnings.filter { it.status == "pending" }.sumOf { it.earned_amount }
                val paidEarnings = earnings.filter { it.status == "paid" }.sumOf { it.earned_amount }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$${String.format("%.0f", totalEarned)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Total Earned", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$${String.format("%.0f", paidEarnings)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                Text("Paid", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.warningContainer)) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$${String.format("%.0f", pendingEarnings)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.warning)
                                Text("Pending", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Edit Percentage (Main Admin)
                if (isMainAdmin && selectedTeacherId != null) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Earnings Percentage", fontWeight = FontWeight.Medium)
                                    val teacher = teachers.find { it.first.user_id == selectedTeacherId }
                                    Text(
                                        "${teacher?.first?.earnings_percentage ?: 0}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(onClick = {
                                    val teacher = teachers.find { it.first.user_id == selectedTeacherId }
                                    newPercentage = teacher?.first?.earnings_percentage?.toString() ?: "25.0"
                                    showEditPercentageDialog = true
                                }) {
                                    Icon(Icons.Filled.Edit, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change")
                                }
                            }
                        }
                    }
                }

                // Earnings History
                item {
                    Text("Earnings History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (earnings.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No earnings recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(earnings) { earning ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Group earning", fontWeight = FontWeight.Medium)
                                    Text("${earning.period_start} - ${earning.period_end}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$${String.format("%.2f", earning.earned_amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("${earning.percentage_applied}% of $${String.format("%.2f", earning.total_payment_amount)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Edit Percentage Dialog
    if (showEditPercentageDialog) {
        AlertDialog(
            onDismissRequest = { showEditPercentageDialog = false },
            title = { Text("Change Earnings Percentage") },
            text = {
                Column {
                    Text("Set the teacher's earnings percentage. This applies to future payments only.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ Previous payment records will NOT be affected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPercentage,
                        onValueChange = { newPercentage = it },
                        label = { Text("Percentage") },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pct = newPercentage.toDoubleOrNull()
                    if (pct != null && pct in 0.0..100.0) {
                        scope.launch {
                            teacherRepo.getTeacherByUserId(selectedTeacherId!!)?.let { teacher ->
                                teacherRepo.updateTeacherPercentage(teacher.id, pct)
                                // Refresh
                                teachers = teacherRepo.getTeachersWithUsers()
                            }
                            showEditPercentageDialog = false
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditPercentageDialog = false }) { Text("Cancel") }
            }
        )
    }
}

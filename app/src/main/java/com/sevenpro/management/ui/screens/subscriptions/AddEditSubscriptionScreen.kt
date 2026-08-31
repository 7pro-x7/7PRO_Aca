package com.sevenpro.management.ui.screens.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.model.*
import com.sevenpro.management.data.repository.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionScreen(
    supabaseClient: SupabaseClient,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val subRepo = remember { SubscriptionRepository(supabaseClient) }
    val userRepo = remember { UserRepository(supabaseClient) }
    val groupRepo = remember { GroupRepository(supabaseClient) }
    val scope = rememberCoroutineScope()

    var parents by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var students by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var groups by remember { mutableStateOf<List<Group>>(emptyList()) }

    var selectedParentId by remember { mutableStateOf<String?>(null) }
    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var monthlyFee by remember { mutableStateOf("") }
    var parentExpanded by remember { mutableStateOf(false) }
    var studentExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            parents = userRepo.getUsersByRole("PARENT")
            students = userRepo.getUsersByRole("STUDENT")
            groups = groupRepo.getActiveGroups()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Subscription", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Parent Selection
            Text("Parent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = parentExpanded, onExpandedChange = { parentExpanded = it }) {
                OutlinedTextField(
                    value = parents.find { it.id == selectedParentId }?.full_name ?: "Select parent",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(parentExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = parentExpanded, onDismissRequest = { parentExpanded = false }) {
                    parents.forEach { parent ->
                        DropdownMenuItem(
                            text = { Text(parent.full_name) },
                            onClick = { selectedParentId = parent.id; parentExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student Selection
            Text("Student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = studentExpanded, onExpandedChange = { studentExpanded = it }) {
                OutlinedTextField(
                    value = students.find { it.id == selectedStudentId }?.full_name ?: "Select student",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(studentExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = studentExpanded, onDismissRequest = { studentExpanded = false }) {
                    students.forEach { student ->
                        DropdownMenuItem(
                            text = { Text(student.full_name) },
                            onClick = { selectedStudentId = student.id; studentExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Group Selection
            Text("Group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(expanded = groupExpanded, onExpandedChange = { groupExpanded = it }) {
                OutlinedTextField(
                    value = groups.find { it.id == selectedGroupId }?.let { "${it.name} - $${String.format("%.0f", it.monthly_fee)}/mo" } ?: "Select group",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text("${group.name} - $${String.format("%.0f", group.monthly_fee)}/mo") },
                            onClick = {
                                selectedGroupId = group.id
                                monthlyFee = group.monthly_fee.toString()
                                groupExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = monthlyFee,
                onValueChange = { monthlyFee = it },
                label = { Text("Monthly Fee ($)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (selectedParentId == null || selectedStudentId == null || selectedGroupId == null) {
                        error = "Please select parent, student, and group"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val today = LocalDate.now()
                            val nextBilling = today.plusMonths(1)
                            subRepo.createSubscription(
                                Subscription(
                                    id = UUID.randomUUID().toString(),
                                    parent_id = selectedParentId!!,
                                    student_id = selectedStudentId!!,
                                    group_id = selectedGroupId!!,
                                    monthly_fee = monthlyFee.toDoubleOrNull() ?: 0.0,
                                    start_date = today.toString(),
                                    next_billing_date = nextBilling.toString()
                                )
                            )
                            isLoading = false
                            onSaved()
                        } catch (e: Exception) {
                            isLoading = false
                            error = e.message ?: "Failed"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("Create Subscription", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

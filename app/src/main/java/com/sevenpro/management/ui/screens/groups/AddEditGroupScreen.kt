package com.sevenpro.management.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.model.Group
import com.sevenpro.management.data.model.TeacherProfile
import com.sevenpro.management.data.model.UserProfile
import com.sevenpro.management.data.repository.GroupRepository
import com.sevenpro.management.data.repository.TeacherRepository
import com.sevenpro.management.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGroupScreen(
    supabaseClient: SupabaseClient,
    editGroupId: String? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val groupRepo = remember { GroupRepository(supabaseClient) }
    val teacherRepo = remember { TeacherRepository(supabaseClient) }
    val userRepo = remember { UserRepository(supabaseClient) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("20") }
    var schedule by remember { mutableStateOf("") }
    var monthlyFee by remember { mutableStateOf("") }
    var selectedTeacherId by remember { mutableStateOf<String?>(null) }
    var teachers by remember { mutableStateOf<List<Pair<TeacherProfile, UserProfile>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val isEditing = editGroupId != null

    LaunchedEffect(Unit) {
        scope.launch {
            teachers = teacherRepo.getTeachersWithUsers()
            if (isEditing) {
                groupRepo.getAllGroups().find { it.id == editGroupId }?.let { group ->
                    name = group.name
                    nameAr = group.name_ar
                    subject = group.subject
                    capacity = group.capacity.toString()
                    schedule = group.schedule
                    monthlyFee = group.monthly_fee.toString()
                    selectedTeacherId = group.teacher_id
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Group" else "Add Group", fontWeight = FontWeight.Bold) },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group Name (English)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = nameAr,
                onValueChange = { nameAr = it },
                label = { Text("Group Name (Arabic)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = schedule,
                onValueChange = { schedule = it },
                label = { Text("Schedule (e.g., Mon/Wed 5-7PM)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = capacity,
                    onValueChange = { capacity = it },
                    label = { Text("Capacity") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = monthlyFee,
                    onValueChange = { monthlyFee = it },
                    label = { Text("Monthly Fee ($)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            // Teacher Assignment
            Spacer(modifier = Modifier.height(16.dp))
            Text("Assign Teacher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = teachers.find { it.first.user_id == selectedTeacherId }?.second?.full_name ?: "No teacher selected",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("No Teacher") },
                        onClick = { selectedTeacherId = null; expanded = false }
                    )
                    teachers.forEach { (teacher, user) ->
                        DropdownMenuItem(
                            text = { Text("${user.full_name} (${teacher.earnings_percentage}%)") },
                            onClick = { selectedTeacherId = user.id; expanded = false }
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isBlank() || subject.isBlank()) {
                        error = "Name and subject are required"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val group = Group(
                                id = editGroupId ?: UUID.randomUUID().toString(),
                                name = name,
                                name_ar = nameAr,
                                subject = subject,
                                teacher_id = selectedTeacherId,
                                capacity = capacity.toIntOrNull() ?: 20,
                                schedule = schedule,
                                monthly_fee = monthlyFee.toDoubleOrNull() ?: 0.0,
                                is_active = true
                            )
                            if (isEditing) groupRepo.updateGroup(group)
                            else groupRepo.createGroup(group)
                            isLoading = false
                            onSaved()
                        } catch (e: Exception) {
                            isLoading = false
                            error = e.message ?: "Failed to save group"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text(if (isEditing) "Save Changes" else "Create Group", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

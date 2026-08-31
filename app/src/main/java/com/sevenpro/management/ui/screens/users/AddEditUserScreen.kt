package com.sevenpro.management.ui.screens.users

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
import com.sevenpro.management.data.model.*
import com.sevenpro.management.data.repository.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditUserScreen(
    supabaseClient: SupabaseClient,
    isMainAdmin: Boolean,
    editUserId: String? = null,
    initialRole: String = "",
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val userRepo = remember { UserRepository(supabaseClient) }
    val authRepo = remember { AuthRepository(supabaseClient) }
    val teacherRepo = remember { TeacherRepository(supabaseClient) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val isEditing = editUserId != null
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(initialRole.ifEmpty { "STUDENT" }) }
    var teacherPercentage by remember { mutableStateOf("25.0") }
    var selectedPermissions by remember { mutableStateOf(setOf<String>()) }

    val roles = listOf("ADMIN", "TEACHER", "STUDENT", "PARENT")
    val allPermissions = Permission.entries.map { it.name }

    LaunchedEffect(editUserId) {
        if (isEditing) {
            scope.launch {
                userRepo.getUserById(editUserId!!)?.let { user ->
                    fullName = user.full_name
                    email = user.email
                    phone = user.phone
                    selectedRole = user.role
                    selectedPermissions = user.permissions.toSet()
                }
                if (selectedRole == "TEACHER") {
                    teacherRepo.getTeacherByUserId(editUserId!!)?.let {
                        teacherPercentage = it.earnings_percentage.toString()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit User" else "Add User", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
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
            // Role Selection
            Text("Role", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(role.replace("_", " "), style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                enabled = !isEditing
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            // Password (only for new users)
            if (!isEditing) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            // Teacher Percentage
            if (selectedRole == "TEACHER") {
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Teacher Earnings Percentage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This percentage is applied to each payment from groups this teacher manages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = teacherPercentage,
                            onValueChange = { teacherPercentage = it },
                            label = { Text("Earnings %") },
                            suffix = { Text("%") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Changing this percentage does not affect previous payment records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            // Permissions (admin only)
            if (selectedRole == "ADMIN" && isMainAdmin) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        allPermissions.forEach { perm ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(perm.replace("_", " "), style = MaterialTheme.typography.bodyMedium)
                                Checkbox(
                                    checked = perm in selectedPermissions,
                                    onCheckedChange = { checked ->
                                        selectedPermissions = if (checked) selectedPermissions + perm
                                        else selectedPermissions - perm
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Save Button
            Button(
                onClick = {
                    if (fullName.isBlank() || email.isBlank()) {
                        error = "Name and email are required"
                        return@Button
                    }
                    if (!isEditing && password.length < 6) {
                        error = "Password must be at least 6 characters"
                        return@Button
                    }
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            if (isEditing) {
                                // Update existing user
                                val existing = userRepo.getUserById(editUserId!!)
                                existing?.let {
                                    userRepo.updateUser(
                                        it.copy(
                                            full_name = fullName,
                                            phone = phone,
                                            role = selectedRole,
                                            permissions = selectedPermissions.toList()
                                        )
                                    )
                                    // Update teacher percentage if applicable
                                    if (selectedRole == "TEACHER") {
                                        val teacher = teacherRepo.getTeacherByUserId(editUserId)
                                        val pct = teacherPercentage.toDoubleOrNull() ?: 25.0
                                        if (teacher != null) {
                                            teacherRepo.updateTeacherPercentage(teacher.id, pct)
                                        } else {
                                            teacherRepo.createTeacher(
                                                TeacherProfile(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    user_id = editUserId,
                                                    earnings_percentage = pct,
                                                    custom_percentage = true
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Create new user
                                authRepo.signUp(email.trim(), password, fullName.trim(), selectedRole)
                                val newUser = userRepo.searchUsers(email.trim()).firstOrNull()
                                newUser?.let {
                                    userRepo.updateUser(
                                        it.copy(phone = phone, permissions = selectedPermissions.toList())
                                    )
                                    if (selectedRole == "TEACHER") {
                                        val pct = teacherPercentage.toDoubleOrNull() ?: 25.0
                                        teacherRepo.createTeacher(
                                            TeacherProfile(
                                                id = java.util.UUID.randomUUID().toString(),
                                                user_id = it.id,
                                                earnings_percentage = pct,
                                                custom_percentage = true
                                            )
                                        )
                                    }
                                }
                            }
                            isLoading = false
                            onSaved()
                        } catch (e: Exception) {
                            isLoading = false
                            error = e.message ?: "Failed to save user"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEditing) "Save Changes" else "Create User", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

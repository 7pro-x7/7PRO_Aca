package com.sevenpro.management.ui.screens.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.sevenpro.management.data.model.UserProfile
import com.sevenpro.management.data.model.TeacherProfile
import com.sevenpro.management.data.repository.UserRepository
import com.sevenpro.management.data.repository.TeacherRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    supabaseClient: SupabaseClient,
    userId: String,
    isMainAdmin: Boolean,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    val userRepo = remember { UserRepository(supabaseClient) }
    val teacherRepo = remember { TeacherRepository(supabaseClient) }
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<UserProfile?>(null) }
    var teacherProfile by remember { mutableStateOf<TeacherProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeactivateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        isLoading = true
        scope.launch {
            user = userRepo.getUserById(userId)
            if (user?.role == "TEACHER") {
                teacherProfile = teacherRepo.getTeacherByUserId(userId)
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isMainAdmin && user?.role != "MAIN_ADMIN") {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("User not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Profile Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    user!!.full_name.take(2).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            user!!.full_name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            user!!.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                user!!.role.replace("_", " "),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!user!!.is_active) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    "INACTIVE",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Cards
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Contact Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        InfoRow("Email", user!!.email)
                        InfoRow("Phone", user!!.phone.ifEmpty { "Not provided" })
                        InfoRow("Joined", user!!.created_at.take(10))
                    }
                }

                // Teacher-specific info
                if (user!!.role == "TEACHER" && teacherProfile != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Teacher Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow("Earnings %", "${teacherProfile!!.earnings_percentage}%")
                            InfoRow("Custom %", if (teacherProfile!!.custom_percentage) "Yes" else "No")
                            InfoRow("Active", if (teacherProfile!!.is_active) "Yes" else "No")
                            if (teacherProfile!!.subjects.isNotEmpty()) {
                                InfoRow("Subjects", teacherProfile!!.subjects.joinToString(", "))
                            }
                        }
                    }
                }

                // Permissions
                if (user!!.permissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Permissions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            user!!.permissions.forEach { perm ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(perm.replace("_", " "), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // Admin Actions
                if (isMainAdmin && user!!.role != "MAIN_ADMIN") {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { showDeactivateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Block, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (user!!.is_active) "Deactivate User" else "Reactivate User")
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Deactivate Dialog
        if (showDeactivateDialog) {
            AlertDialog(
                onDismissRequest = { showDeactivateDialog = false },
                title = { Text("Confirm Action") },
                text = {
                    Text(
                        if (user?.is_active == true) "Deactivate this user? They won't be able to log in."
                        else "Reactivate this user?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            user?.let {
                                userRepo.deactivateUser(it.id)
                                user = it.copy(is_active = !it.is_active)
                            }
                            showDeactivateDialog = false
                        }
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeactivateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

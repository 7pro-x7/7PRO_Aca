package com.sevenpro.management.ui.screens.notifications

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.local.UserPreferences
import com.sevenpro.management.data.model.AppNotification
import com.sevenpro.management.data.repository.NotificationRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    supabaseClient: SupabaseClient,
    userPreferences: UserPreferences,
    onBack: () -> Unit
) {
    val notifRepo = remember { NotificationRepository(supabaseClient) }
    val scope = rememberCoroutineScope()
    val userId by userPreferences.userId.collectAsState(initial = null)

    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val language by userPreferences.language.collectAsState(initial = "en")

    LaunchedEffect(userId) {
        userId?.let { uid ->
            scope.launch {
                notifications = notifRepo.getUserNotifications(uid)
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (notifications.any { !it.is_read }) {
                        TextButton(onClick = {
                            scope.launch {
                                notifications.filter { !it.is_read }.forEach {
                                    notifRepo.markAsRead(it.id)
                                }
                                notifications = notifications.map { it.copy(is_read = true) }
                            }
                        }) {
                            Text("Mark all read")
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
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No notifications", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.is_read) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        onClick = {
                            scope.launch {
                                if (!notif.is_read) {
                                    notifRepo.markAsRead(notif.id)
                                    notifications = notifications.map {
                                        if (it.id == notif.id) it.copy(is_read = true) else it
                                    }
                                }
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                when (notif.type) {
                                    "payment" -> Icons.Filled.Payments
                                    "subscription" -> Icons.Filled.CardMembership
                                    "alert" -> Icons.Filled.Warning
                                    else -> Icons.Filled.Info
                                },
                                null,
                                tint = when (notif.type) {
                                    "payment" -> MaterialTheme.colorScheme.primary
                                    "subscription" -> MaterialTheme.colorScheme.tertiary
                                    "alert" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (language == "ar") notif.title_ar.ifEmpty { notif.title } else notif.title,
                                    fontWeight = if (!notif.is_read) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (language == "ar") notif.message_ar.ifEmpty { notif.message } else notif.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!notif.is_read) {
                                Box(
                                    modifier = Modifier.size(8.dp).padding(top = 4.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    // Use surface instead of colored background for the dot
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    ) {}
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

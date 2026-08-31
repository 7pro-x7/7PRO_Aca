package com.sevenpro.management.ui.screens.subscriptions

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
import com.sevenpro.management.data.model.Subscription
import com.sevenpro.management.data.repository.SubscriptionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    supabaseClient: SupabaseClient,
    isAdmin: Boolean,
    onSubscriptionClick: (String) -> Unit,
    onAddSubscription: () -> Unit
) {
    val subRepo = remember { SubscriptionRepository(supabaseClient) }
    val scope = rememberCoroutineScope()

    var subscriptions by remember { mutableStateOf<List<Subscription>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatus by remember { mutableStateOf("all") }

    LaunchedEffect(selectedStatus) {
        isLoading = true
        scope.launch {
            try {
                subscriptions = when (selectedStatus) {
                    "active" -> subRepo.getActiveSubscriptions()
                    else -> subRepo.getAllSubscriptions()
                }
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions", fontWeight = FontWeight.Bold) },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = onAddSubscription) {
                            Icon(Icons.Filled.Add, "Add Subscription")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Status Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "active" to "Active", "paused" to "Paused", "cancelled" to "Cancelled").forEach { (status, label) ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(label) }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (subscriptions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CardMembership, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No subscriptions", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subscriptions) { sub ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onSubscriptionClick(sub.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CardMembership, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Subscription #${sub.id.take(8)}", fontWeight = FontWeight.Medium)
                                    Text("Next billing: ${sub.next_billing_date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$${String.format("%.2f", sub.monthly_fee)}/mo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    StatusBadge(sub.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = when (status) {
            "active" -> MaterialTheme.colorScheme.tertiaryContainer
            "paused" -> MaterialTheme.colorScheme.secondaryContainer
            "cancelled" -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            status.replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = when (status) {
                "active" -> MaterialTheme.colorScheme.tertiary
                "paused" -> MaterialTheme.colorScheme.secondary
                "cancelled" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

package com.sevenpro.management.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.local.UserPreferences
import com.sevenpro.management.data.model.*
import com.sevenpro.management.data.repository.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    supabaseClient: SupabaseClient,
    userPreferences: UserPreferences,
    isMainAdmin: Boolean,
    onNavigateToPayment: (String) -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val financialRepo = remember { FinancialRepository(supabaseClient) }
    val paymentRepo = remember { PaymentRepository(supabaseClient) }
    val userRepo = remember { UserRepository(supabaseClient) }
    val groupRepo = remember { GroupRepository(supabaseClient) }
    val subRepo = remember { SubscriptionRepository(supabaseClient) }
    val earningRepo = remember { EarningsRepository(supabaseClient) }

    var stats by remember { mutableStateOf(DashboardStats()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedPeriod by remember { mutableStateOf("month") }

    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch {
            try {
                val today = LocalDate.now()
                val monthStart = today.withDayOfMonth(1).toString()
                val monthEnd = today.toString()

                val revenue = financialRepo.getTotalRevenue()
                val expenses = financialRepo.getTotalExpenses()
                val teacherPayments = financialRepo.getTotalTeacherPayments()
                val overdue = paymentRepo.getOverduePayments()
                val upcoming = paymentRepo.getUpcomingPayments()
                val transactions = financialRepo.getAllTransactions().take(10)
                val revenueByMonth = financialRepo.getRevenueByMonth()
                val expensesByMonth = financialRepo.getExpensesByMonth()
                val activeSubs = subRepo.getActiveSubscriptions().size
                val users = userRepo.getAllUsers()
                val teachers = users.count { it.role == "TEACHER" }
                val students = users.count { it.role == "STUDENT" }
                val parents = users.count { it.role == "PARENT" }
                val groups = groupRepo.getAllGroups().size

                stats = DashboardStats(
                    totalRevenue = revenue,
                    totalExpenses = expenses,
                    netProfit = revenue - expenses - teacherPayments,
                    totalTeacherEarnings = teacherPayments,
                    activeSubscriptions = activeSubs,
                    totalStudents = students,
                    totalTeachers = teachers,
                    totalParents = parents,
                    totalGroups = groups,
                    overduePayments = overdue,
                    upcomingPayments = upcoming,
                    recentTransactions = transactions,
                    revenueByMonth = revenueByMonth,
                    expensesByMonth = expensesByMonth
                )
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("7PRO Dashboard", fontWeight = FontWeight.Bold)
                        Text(
                            "Management Overview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        BadgedBox(
                            badge = {
                                Badge { Text("!") }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Revenue Overview Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Revenue",
                            value = "$${String.format("%.0f", stats.totalRevenue)}",
                            icon = Icons.Filled.TrendingUp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Expenses",
                            value = "$${String.format("%.0f", stats.totalExpenses)}",
                            icon = Icons.Filled.TrendingDown,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Net Profit",
                            value = "$${String.format("%.0f", stats.netProfit)}",
                            icon = Icons.Filled.AccountBalance,
                            color = if (stats.netProfit >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "Teacher Earnings",
                            value = "$${String.format("%.0f", stats.totalTeacherEarnings)}",
                            icon = Icons.Filled.Groups,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Platform Stats
                item {
                    Text(
                        "Platform Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniStatCard("Teachers", stats.totalTeachers.toString(), Icons.Outlined.School, Modifier.weight(1f))
                        MiniStatCard("Students", stats.totalStudents.toString(), Icons.Outlined.Person, Modifier.weight(1f))
                        MiniStatCard("Parents", stats.totalParents.toString(), Icons.Outlined.FamilyRestroom, Modifier.weight(1f))
                        MiniStatCard("Groups", stats.totalGroups.toString(), Icons.Outlined.Groups, Modifier.weight(1f))
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CardMembership,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "${stats.activeSubscriptions}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Active Subscriptions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Overdue Payments Alert
                if (stats.overduePayments.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${stats.overduePayments.size} Overdue Payments",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                stats.overduePayments.take(3).forEach { payment ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onNavigateToPayment(payment.id) }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            payment.notes.ifEmpty { "Payment #${payment.id.take(8)}" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            "$${String.format("%.2f", payment.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Upcoming Payments
                if (stats.upcomingPayments.isNotEmpty()) {
                    item {
                        Text(
                            "Upcoming Payments",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(stats.upcomingPayments.take(5)) { payment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPayment(payment.id) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Payment,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        payment.notes.ifEmpty { "Payment" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Due: ${payment.due_date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$${String.format("%.2f", payment.amount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.warningContainer
                                    ) {
                                        Text(
                                            "Pending",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Revenue Chart
                item {
                    Text(
                        "Revenue vs Expenses",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (stats.revenueByMonth.isEmpty() && stats.expensesByMonth.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No data yet. Start recording transactions.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                // Simple bar chart
                                val allMonths = (stats.revenueByMonth.keys + stats.expensesByMonth.keys).sorted().takeLast(6)
                                allMonths.forEach { month ->
                                    val rev = stats.revenueByMonth[month] ?: 0.0
                                    val exp = stats.expensesByMonth[month] ?: 0.0
                                    val maxVal = maxOf(rev, exp, 1.0)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            month.takeLast(2),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.width(30.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            // Revenue bar
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth((rev / maxVal).toFloat().coerceIn(0.05f, 1f))
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.tertiary)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            // Expense bar
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth((exp / maxVal).toFloat().coerceIn(0.05f, 1f))
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.width(80.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                "$${String.format("%.0f", rev)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                "$${String.format("%.0f", exp)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    LegendDot(MaterialTheme.colorScheme.tertiary, "Revenue")
                                    Spacer(modifier = Modifier.width(16.dp))
                                    LegendDot(MaterialTheme.colorScheme.error, "Expenses")
                                }
                            }
                        }
                    }
                }

                // Recent Transactions
                if (stats.recentTransactions.isNotEmpty()) {
                    item {
                        Text(
                            "Recent Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(stats.recentTransactions.take(5)) { txn ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (txn.type) {
                                                "revenue" -> MaterialTheme.colorScheme.tertiaryContainer
                                                "expense" -> MaterialTheme.colorScheme.errorContainer
                                                else -> MaterialTheme.colorScheme.secondaryContainer
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (txn.type) {
                                            "revenue" -> Icons.Filled.ArrowDownward
                                            "expense" -> Icons.Filled.ArrowUpward
                                            else -> Icons.Filled.SwapHoriz
                                        },
                                        contentDescription = null,
                                        tint = when (txn.type) {
                                            "revenue" -> MaterialTheme.colorScheme.tertiary
                                            "expense" -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.secondary
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        txn.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        txn.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${if (txn.type == "revenue") "+" else "-"}$${String.format("%.2f", txn.amount)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (txn.type == "revenue") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MiniStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

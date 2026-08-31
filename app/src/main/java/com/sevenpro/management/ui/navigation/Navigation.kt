package com.sevenpro.management.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import io.github.jan.supabase.SupabaseClient
import com.sevenpro.management.data.local.UserPreferences
import com.sevenpro.management.data.model.UserRole
import com.sevenpro.management.ui.screens.auth.LoginScreen
import com.sevenpro.management.ui.screens.auth.RegisterScreen
import com.sevenpro.management.ui.screens.dashboard.DashboardScreen
import com.sevenpro.management.ui.screens.users.UserListScreen
import com.sevenpro.management.ui.screens.users.UserDetailScreen
import com.sevenpro.management.ui.screens.users.AddEditUserScreen
import com.sevenpro.management.ui.screens.groups.GroupListScreen
import com.sevenpro.management.ui.screens.groups.AddEditGroupScreen
import com.sevenpro.management.ui.screens.subscriptions.SubscriptionListScreen
import com.sevenpro.management.ui.screens.subscriptions.AddEditSubscriptionScreen
import com.sevenpro.management.ui.screens.payments.PaymentListScreen
import com.sevenpro.management.ui.screens.payments.PaymentDetailScreen
import com.sevenpro.management.ui.screens.earnings.TeacherEarningsScreen
import com.sevenpro.management.ui.screens.earnings.TeacherPaymentScreen
import com.sevenpro.management.ui.screens.reports.ReportsScreen
import com.sevenpro.management.ui.screens.settings.SettingsScreen
import com.sevenpro.management.ui.screens.notifications.NotificationScreen

sealed class Screen(val route: String, val title: String, val titleAr: String) {
    object Login : Screen("login", "Login", "تسجيل الدخول")
    object Register : Screen("register", "Register", "إنشاء حساب")
    object Dashboard : Screen("dashboard", "Dashboard", "لوحة التحكم")
    object Users : Screen("users", "Users", "المستخدمون")
    object UserDetail : Screen("users/{userId}", "User Details", "تفاصيل المستخدم") {
        fun createRoute(userId: String) = "users/$userId"
    }
    object AddUser : Screen("add_user?role={role}", "Add User", "إضافة مستخدم") {
        fun createRoute(role: String = "") = "add_user?role=$role"
    }
    object EditUser : Screen("edit_user/{userId}", "Edit User", "تعديل المستخدم") {
        fun createRoute(userId: String) = "edit_user/$userId"
    }
    object Groups : Screen("groups", "Groups", "المجموعات")
    object AddGroup : Screen("add_group", "Add Group", "إضافة مجموعة")
    object EditGroup : Screen("edit_group/{groupId}", "Edit Group", "تعديل مجموعة") {
        fun createRoute(groupId: String) = "edit_group/$groupId"
    }
    object Subscriptions : Screen("subscriptions", "Subscriptions", "الاشتراكات")
    object AddSubscription : Screen("add_subscription", "Add Subscription", "إضافة اشتراك")
    object EditSubscription : Screen("edit_subscription/{subId}", "Edit Subscription", "تعديل اشتراك") {
        fun createRoute(subId: String) = "edit_subscription/$subId"
    }
    object Payments : Screen("payments", "Payments", "المدفوعات")
    object PaymentDetail : Screen("payments/{paymentId}", "Payment Details", "تفاصيل الدفع") {
        fun createRoute(paymentId: String) = "payments/$paymentId"
    }
    object Earnings : Screen("earnings", "Teacher Earnings", "أرباح المعلمين")
    object TeacherPayments : Screen("teacher_payments", "Teacher Payments", "مدفوعات المعلمين")
    object Reports : Screen("reports", "Reports", "التقارير")
    object Settings : Screen("settings", "Settings", "الإعدادات")
    object Notifications : Screen("notifications", "Notifications", "الإشعارات")
}

data class NavItem(
    val screen: Screen,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val adminNavItems = listOf(
    NavItem(Screen.Dashboard, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    NavItem(Screen.Users, Icons.Outlined.People, Icons.Filled.People),
    NavItem(Screen.Groups, Icons.Outlined.Groups, Icons.Filled.Groups),
    NavItem(Screen.Subscriptions, Icons.Outlined.CardMembership, Icons.Filled.CardMembership),
    NavItem(Screen.Payments, Icons.Outlined.Payments, Icons.Filled.Payments),
    NavItem(Screen.Earnings, Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    NavItem(Screen.Reports, Icons.Outlined.Assessment, Icons.Filled.Assessment),
    NavItem(Screen.Settings, Icons.Outlined.Settings, Icons.Filled.Settings)
)

val teacherNavItems = listOf(
    NavItem(Screen.Dashboard, Icons.Outlined.Dashboard, Icons.Filled.Dashboard),
    NavItem(Screen.Earnings, Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    NavItem(Screen.TeacherPayments, Icons.Outlined.Receipt, Icons.Filled.Receipt),
    NavItem(Screen.Settings, Icons.Outlined.Settings, Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SevenProNavHost(
    userPreferences: UserPreferences,
    supabaseClient: SupabaseClient
) {
    val navController = rememberNavController()
    val isLoggedIn by userPreferences.isLoggedIn.collectAsState(initial = false)
    val userRole by userPreferences.userRole.collectAsState(initial = null)

    val startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    val isMainAdmin = userRole == "MAIN_ADMIN"
    val isAdmin = userRole == "ADMIN" || isMainAdmin

    Scaffold(
        bottomBar = {
            if (isLoggedIn) {
                val navItems = if (isAdmin) adminNavItems else teacherNavItems
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (navItems.any { it.screen.route == currentRoute }) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != item.screen.route) {
                                        navController.navigate(item.screen.route) {
                                            popUpTo(Screen.Dashboard.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.icon,
                                        contentDescription = null
                                    )
                                },
                                label = {
                                    Text(
                                        if (isAdmin) item.screen.title else item.screen.titleAr
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (isAdmin && currentRoute == Screen.Dashboard.route) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.Notifications.route) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { it / 4 } },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally { -it / 4 } },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    supabaseClient = supabaseClient,
                    userPreferences = userPreferences,
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    supabaseClient = supabaseClient,
                    userPreferences = userPreferences,
                    onRegisterSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    supabaseClient = supabaseClient,
                    userPreferences = userPreferences,
                    isMainAdmin = isMainAdmin,
                    onNavigateToPayment = { navController.navigate(Screen.PaymentDetail.createRoute(it)) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) }
                )
            }

            composable(Screen.Users.route) {
                UserListScreen(
                    supabaseClient = supabaseClient,
                    isMainAdmin = isMainAdmin,
                    onUserClick = { navController.navigate(Screen.UserDetail.createRoute(it)) },
                    onAddUser = { navController.navigate(Screen.AddUser.createRoute()) }
                )
            }

            composable(
                Screen.UserDetail.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                UserDetailScreen(
                    supabaseClient = supabaseClient,
                    userId = userId,
                    isMainAdmin = isMainAdmin,
                    onEdit = { navController.navigate(Screen.EditUser.createRoute(userId)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.AddUser.route,
                arguments = listOf(navArgument("role") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val role = backStackEntry.arguments?.getString("role") ?: ""
                AddEditUserScreen(
                    supabaseClient = supabaseClient,
                    isMainAdmin = isMainAdmin,
                    initialRole = role,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.EditUser.route,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: ""
                AddEditUserScreen(
                    supabaseClient = supabaseClient,
                    isMainAdmin = isMainAdmin,
                    editUserId = userId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Groups.route) {
                GroupListScreen(
                    supabaseClient = supabaseClient,
                    isAdmin = isAdmin,
                    onGroupClick = { },
                    onAddGroup = { navController.navigate(Screen.AddGroup.route) }
                )
            }

            composable(Screen.AddGroup.route) {
                AddEditGroupScreen(
                    supabaseClient = supabaseClient,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                Screen.EditGroup.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                AddEditGroupScreen(
                    supabaseClient = supabaseClient,
                    editGroupId = groupId,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Subscriptions.route) {
                SubscriptionListScreen(
                    supabaseClient = supabaseClient,
                    isAdmin = isAdmin,
                    onSubscriptionClick = { },
                    onAddSubscription = { navController.navigate(Screen.AddSubscription.route) }
                )
            }

            composable(Screen.AddSubscription.route) {
                AddEditSubscriptionScreen(
                    supabaseClient = supabaseClient,
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Payments.route) {
                PaymentListScreen(
                    supabaseClient = supabaseClient,
                    isAdmin = isAdmin,
                    onPaymentClick = { navController.navigate(Screen.PaymentDetail.createRoute(it)) }
                )
            }

            composable(
                Screen.PaymentDetail.route,
                arguments = listOf(navArgument("paymentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val paymentId = backStackEntry.arguments?.getString("paymentId") ?: ""
                PaymentDetailScreen(
                    supabaseClient = supabaseClient,
                    paymentId = paymentId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Earnings.route) {
                TeacherEarningsScreen(
                    supabaseClient = supabaseClient,
                    userPreferences = userPreferences,
                    isAdmin = isAdmin,
                    isMainAdmin = isMainAdmin
                )
            }

            composable(Screen.TeacherPayments.route) {
                TeacherPaymentScreen(
                    supabaseClient = supabaseClient,
                    userPreferences = userPreferences
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    supabaseClient = supabaseClient,
                    isMainAdmin = isMainAdmin
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    supabaseClient = supabaseClient,
                    userPreferences = userPreferences,
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationScreen(
                    supabaseClient = supabaseClient,
                    userPreferences = userPreferences,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

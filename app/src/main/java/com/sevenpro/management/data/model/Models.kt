package com.sevenpro.management.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Roles & Permissions ──────────────────────────────────────────────────────

enum class UserRole {
    MAIN_ADMIN, ADMIN, TEACHER, PARENT, STUDENT
}

@Serializable
enum class Permission {
    MANAGE_USERS,
    MANAGE_TEACHERS,
    MANAGE_STUDENTS,
    MANAGE_PARENTS,
    MANAGE_GROUPS,
    MANAGE_SUBSCRIPTIONS,
    MANAGE_PAYMENTS,
    MANAGE_EARNINGS,
    VIEW_REPORTS,
    MANAGE_SETTINGS,
    MANAGE_ADMINS,
    EXPORT_DATA
}

// ─── Users ────────────────────────────────────────────────────────────────────

@Serializable
data class UserProfile(
    val id: String,
    val email: String = "",
    val full_name: String = "",
    val role: String = "STUDENT",
    val phone: String = "",
    val is_active: Boolean = true,
    val permissions: List<String> = emptyList(),
    val created_at: String = "",
    val avatar_url: String? = null
)

@Serializable
data class TeacherProfile(
    val id: String,
    val user_id: String,
    val earnings_percentage: Double = 0.0,
    val custom_percentage: Boolean = false,
    val is_active: Boolean = true,
    val subjects: List<String> = emptyList(),
    val bio: String = "",
    val created_at: String = ""
)

@Serializable
data class ParentProfile(
    val id: String,
    val user_id: String,
    val children_ids: List<String> = emptyList(),
    val created_at: String = ""
)

@Serializable
data class StudentProfile(
    val id: String,
    val user_id: String,
    val parent_id: String? = null,
    val grade_level: String = "",
    val created_at: String = ""
)

// ─── Groups ───────────────────────────────────────────────────────────────────

@Serializable
data class Group(
    val id: String,
    val name: String = "",
    val name_ar: String = "",
    val subject: String = "",
    val teacher_id: String? = null,
    val capacity: Int = 20,
    val schedule: String = "",
    val monthly_fee: Double = 0.0,
    val is_active: Boolean = true,
    val created_at: String = ""
)

@Serializable
data class GroupStudent(
    val id: String,
    val group_id: String,
    val student_id: String,
    val joined_at: String = ""
)

// ─── Subscriptions ────────────────────────────────────────────────────────────

@Serializable
enum class SubscriptionStatus {
    @SerialName("active") ACTIVE,
    @SerialName("paused") PAUSED,
    @SerialName("cancelled") CANCELLED,
    @SerialName("expired") EXPIRED
}

@Serializable
data class Subscription(
    val id: String,
    val parent_id: String,
    val student_id: String,
    val group_id: String,
    val status: String = "active",
    val monthly_fee: Double = 0.0,
    val start_date: String = "",
    val next_billing_date: String = "",
    val last_payment_date: String? = null,
    val auto_renew: Boolean = true,
    val created_at: String = ""
)

// ─── Payments ─────────────────────────────────────────────────────────────────

@Serializable
enum class PaymentStatus {
    @SerialName("paid") PAID,
    @SerialName("pending") PENDING,
    @SerialName("overdue") OVERDUE,
    @SerialName("partial") PARTIAL,
    @SerialName("refunded") REFUNDED
}

@Serializable
data class Payment(
    val id: String,
    val subscription_id: String,
    val parent_id: String,
    val student_id: String,
    val group_id: String,
    val amount: Double = 0.0,
    val status: String = "pending",
    val due_date: String = "",
    val paid_date: String? = null,
    val payment_method: String? = null,
    val reference_number: String? = null,
    val notes: String = "",
    val created_at: String = ""
)

// ─── Teacher Earnings ─────────────────────────────────────────────────────────

@Serializable
data class TeacherEarning(
    val id: String,
    val teacher_id: String,
    val payment_id: String,
    val group_id: String,
    val total_payment_amount: Double = 0.0,
    val percentage_applied: Double = 0.0,
    val earned_amount: Double = 0.0,
    val period_start: String = "",
    val period_end: String = "",
    val status: String = "pending",
    val created_at: String = ""
)

@Serializable
data class TeacherPayment(
    val id: String,
    val teacher_id: String,
    val total_amount: Double = 0.0,
    val period_start: String = "",
    val period_end: String = "",
    val status: String = "pending",
    val paid_date: String? = null,
    val payment_method: String? = null,
    val notes: String = "",
    val created_at: String = ""
)

// ─── Financial Records ────────────────────────────────────────────────────────

@Serializable
enum class TransactionType {
    @SerialName("revenue") REVENUE,
    @SerialName("expense") EXPENSE,
    @SerialName("teacher_payment") TEACHER_PAYMENT,
    @SerialName("refund") REFUND,
    @SerialName("other") OTHER
}

@Serializable
data class Transaction(
    val id: String,
    val type: String = "other",
    val amount: Double = 0.0,
    val description: String = "",
    val reference_id: String? = null,
    val reference_type: String? = null,
    val category: String = "",
    val created_by: String = "",
    val created_at: String = "",
    val is_deleted: Boolean = false,
    val deleted_at: String? = null
)

@Serializable
data class Expense(
    val id: String,
    val category: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val receipt_url: String? = null,
    val created_by: String = "",
    val created_at: String = ""
)

// ─── Audit Log ────────────────────────────────────────────────────────────────

@Serializable
data class AuditLog(
    val id: String,
    val user_id: String = "",
    val action: String = "",
    val entity_type: String = "",
    val entity_id: String = "",
    val old_value: String? = null,
    val new_value: String? = null,
    val ip_address: String? = null,
    val created_at: String = ""
)

// ─── Notifications ────────────────────────────────────────────────────────────

@Serializable
data class AppNotification(
    val id: String,
    val user_id: String = "",
    val title: String = "",
    val title_ar: String = "",
    val message: String = "",
    val message_ar: String = "",
    val type: String = "",
    val is_read: Boolean = false,
    val reference_id: String? = null,
    val created_at: String = ""
)

// ─── Dashboard Stats ──────────────────────────────────────────────────────────

data class DashboardStats(
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val totalTeacherEarnings: Double = 0.0,
    val activeSubscriptions: Int = 0,
    val totalStudents: Int = 0,
    val totalTeachers: Int = 0,
    val totalParents: Int = 0,
    val totalGroups: Int = 0,
    val overduePayments: List<Payment> = emptyList(),
    val upcomingPayments: List<Payment> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val revenueByMonth: Map<String, Double> = emptyMap(),
    val expensesByMonth: Map<String, Double> = emptyMap()
)

// ─── Report Models ────────────────────────────────────────────────────────────

data class FinancialReport(
    val periodStart: String,
    val periodEnd: String,
    val totalRevenue: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val teacherEarnings: Double,
    val subscriptionCount: Int,
    val paymentBreakdown: Map<String, Double>,
    val transactions: List<Transaction>
)

enum class ExportFormat {
    PDF, CSV, EXCEL
}

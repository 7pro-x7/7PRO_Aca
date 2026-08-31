package com.sevenpro.management.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.*
import com.sevenpro.management.data.model.*
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// ─── Auth Repository ──────────────────────────────────────────────────────────

class AuthRepository(private val client: SupabaseClient) {

    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        val session = client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        session.user?.id ?: throw Exception("Auth failed")
    }

    suspend fun signUp(email: String, password: String, fullName: String, role: String = "STUDENT"): Result<String> = runCatching {
        val session = client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName)
                put("role", role)
            }
        }
        val userId = session.user?.id ?: throw Exception("Sign up failed")
        // Create user profile
        client.from("user_profiles").insert(
            UserProfile(
                id = userId,
                email = email,
                full_name = fullName,
                role = role,
                permissions = defaultPermissions(role)
            )
        )
        userId
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    suspend fun getCurrentUser(): UserProfile? = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: return@runCatching null
        client.from("user_profiles").select {
            filter { eq("id", userId) }
        }.decodeSingle<UserProfile>()
    }.getOrNull()

    private fun defaultPermissions(role: String): List<String> = when (role) {
        "MAIN_ADMIN" -> Permission.entries.map { it.name }
        "ADMIN" -> listOf(
            Permission.MANAGE_USERS.name,
            Permission.MANAGE_TEACHERS.name,
            Permission.MANAGE_STUDENTS.name,
            Permission.MANAGE_PARENTS.name,
            Permission.MANAGE_GROUPS.name,
            Permission.MANAGE_SUBSCRIPTIONS.name,
            Permission.MANAGE_PAYMENTS.name,
            Permission.VIEW_REPORTS.name,
            Permission.EXPORT_DATA.name
        )
        "TEACHER" -> listOf(Permission.VIEW_REPORTS.name)
        else -> emptyList()
    }
}

// ─── User Repository ──────────────────────────────────────────────────────────

class UserRepository(private val client: SupabaseClient) {

    suspend fun getAllUsers(): List<UserProfile> = client.from("user_profiles")
        .select { order("created_at", Order.DESCENDING) }
        .decodeList()

    suspend fun getUsersByRole(role: String): List<UserProfile> = client.from("user_profiles")
        .select {
            filter { eq("role", role) }
            order("created_at", Order.DESCENDING)
        }
        .decodeList()

    suspend fun getUserById(id: String): UserProfile? = runCatching {
        client.from("user_profiles").select {
            filter { eq("id", id) }
        }.decodeSingle<UserProfile>()
    }.getOrNull()

    suspend fun updateUser(user: UserProfile) {
        client.from("user_profiles").update(user) {
            filter { eq("id", user.id) }
        }
    }

    suspend fun deactivateUser(id: String) {
        client.from("user_profiles").update(
            mapOf("is_active" to false)
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun searchUsers(query: String): List<UserProfile> = client.from("user_profiles")
        .select {
            filter {
                or {
                    ilike("full_name", "%$query%")
                    ilike("email", "%$query%")
                }
            }
        }
        .decodeList()

    suspend fun updateUserPermissions(userId: String, permissions: List<String>) {
        client.from("user_profiles").update(
            mapOf("permissions" to permissions)
        ) {
            filter { eq("id", userId) }
        }
    }
}

// ─── Teacher Repository ───────────────────────────────────────────────────────

class TeacherRepository(private val client: SupabaseClient) {

    suspend fun getTeacherByUserId(userId: String): TeacherProfile? = runCatching {
        client.from("teacher_profiles").select {
            filter { eq("user_id", userId) }
        }.decodeSingle<TeacherProfile>()
    }.getOrNull()

    suspend fun getAllTeachers(): List<TeacherProfile> = client.from("teacher_profiles")
        .select { order("created_at", Order.DESCENDING) }
        .decodeList()

    suspend fun createTeacher(teacher: TeacherProfile): TeacherProfile {
        client.from("teacher_profiles").insert(teacher)
        return teacher
    }

    suspend fun updateTeacherPercentage(teacherId: String, percentage: Double) {
        client.from("teacher_profiles").update(
            mapOf(
                "earnings_percentage" to percentage,
                "custom_percentage" to true
            )
        ) {
            filter { eq("id", teacherId) }
        }
    }

    suspend fun getTeachersWithUsers(): List<Pair<TeacherProfile, UserProfile>> {
        val teachers = getAllTeachers()
        val users = UserRepository(client).getAllUsers()
        val userMap = users.associateBy { it.id }
        return teachers.mapNotNull { t ->
            userMap[t.user_id]?.let { u -> t to u }
        }
    }

    suspend fun searchTeachers(query: String): List<TeacherProfile> {
        val users = UserRepository(client).searchUsers(query)
        val teacherUserIds = users.filter { it.role == "TEACHER" }.map { it.id }
        if (teacherUserIds.isEmpty()) return emptyList()
        return client.from("teacher_profiles").select {
            filter { isIn("user_id", teacherUserIds) }
        }.decodeList()
    }
}

// ─── Group Repository ─────────────────────────────────────────────────────────

class GroupRepository(private val client: SupabaseClient) {

    suspend fun getAllGroups(): List<Group> = client.from("groups")
        .select { order("created_at", Order.DESCENDING) }
        .decodeList()

    suspend fun getActiveGroups(): List<Group> = client.from("groups")
        .select {
            filter { eq("is_active", true) }
            order("name")
        }
        .decodeList()

    suspend fun createGroup(group: Group): Group {
        val id = UUID.randomUUID().toString()
        val newGroup = group.copy(id = id)
        client.from("groups").insert(newGroup)
        return newGroup
    }

    suspend fun updateGroup(group: Group) {
        client.from("groups").update(group) {
            filter { eq("id", group.id) }
        }
    }

    suspend fun deleteGroup(id: String) {
        client.from("groups").update(
            mapOf("is_active" to false)
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun addStudentToGroup(groupId: String, studentId: String) {
        client.from("group_students").insert(
            GroupStudent(
                id = UUID.randomUUID().toString(),
                group_id = groupId,
                student_id = studentId
            )
        )
    }

    suspend fun removeStudentFromGroup(groupId: String, studentId: String) {
        client.from("group_students").delete {
            filter {
                eq("group_id", groupId)
                eq("student_id", studentId)
            }
        }
    }

    suspend fun getGroupStudents(groupId: String): List<GroupStudent> = client.from("group_students")
        .select {
            filter { eq("group_id", groupId) }
        }
        .decodeList()

    suspend fun searchGroups(query: String): List<Group> = client.from("groups")
        .select {
            filter {
                or {
                    ilike("name", "%$query%")
                    ilike("subject", "%$query%")
                }
            }
        }
        .decodeList()
}

// ─── Subscription Repository ──────────────────────────────────────────────────

class SubscriptionRepository(private val client: SupabaseClient) {

    suspend fun getAllSubscriptions(): List<Subscription> = client.from("subscriptions")
        .select { order("created_at", Order.DESCENDING) }
        .decodeList()

    suspend fun getActiveSubscriptions(): List<Subscription> = client.from("subscriptions")
        .select {
            filter { eq("status", "active") }
        }
        .decodeList()

    suspend fun getSubscriptionsByParent(parentId: String): List<Subscription> =
        client.from("subscriptions").select {
            filter { eq("parent_id", parentId) }
        }.decodeList()

    suspend fun createSubscription(sub: Subscription): Subscription {
        val id = UUID.randomUUID().toString()
        val newSub = sub.copy(id = id)
        client.from("subscriptions").insert(newSub)
        return newSub
    }

    suspend fun updateSubscriptionStatus(subId: String, status: String) {
        client.from("subscriptions").update(
            mapOf("status" to status)
        ) {
            filter { eq("id", subId) }
        }
    }

    suspend fun updateNextBillingDate(subId: String, date: String) {
        client.from("subscriptions").update(
            mapOf(
                "next_billing_date" to date,
                "last_payment_date" to LocalDate.now().toString()
            )
        ) {
            filter { eq("id", subId) }
        }
    }

    suspend fun getSubscriptionsDueForRenewal(date: String): List<Subscription> =
        client.from("subscriptions").select {
            filter {
                eq("status", "active")
                lte("next_billing_date", date)
            }
        }.decodeList()

    suspend fun pauseSubscription(subId: String) {
        updateSubscriptionStatus(subId, "paused")
    }

    suspend fun cancelSubscription(subId: String) {
        updateSubscriptionStatus(subId, "cancelled")
    }
}

// ─── Payment Repository ───────────────────────────────────────────────────────

class PaymentRepository(private val client: SupabaseClient) {

    suspend fun getAllPayments(): List<Payment> = client.from("payments")
        .select { order("due_date", Order.DESCENDING) }
        .decodeList()

    suspend fun getPaymentsByStatus(status: String): List<Payment> = client.from("payments")
        .select {
            filter { eq("status", status) }
            order("due_date")
        }
        .decodeList()

    suspend fun getOverduePayments(): List<Payment> {
        val today = LocalDate.now().toString()
        return client.from("payments").select {
            filter {
                eq("status", "pending")
                lt("due_date", today)
            }
        }.decodeList()
    }

    suspend fun getUpcomingPayments(days: Int = 7): List<Payment> {
        val today = LocalDate.now().toString()
        val futureDate = LocalDate.now().plusDays(days.toLong()).toString()
        return client.from("payments").select {
            filter {
                eq("status", "pending")
                gte("due_date", today)
                lte("due_date", futureDate)
            }
        }.decodeList()
    }

    suspend fun createPayment(payment: Payment): Payment {
        val id = UUID.randomUUID().toString()
        val newPayment = payment.copy(id = id)
        client.from("payments").insert(newPayment)
        return newPayment
    }

    suspend fun markAsPaid(paymentId: String, method: String) {
        client.from("payments").update(
            mapOf(
                "status" to "paid",
                "paid_date" to LocalDate.now().toString(),
                "payment_method" to method
            )
        ) {
            filter { eq("id", paymentId) }
        }
    }

    suspend fun getPaymentsByGroup(groupId: String): List<Payment> = client.from("payments")
        .select {
            filter { eq("group_id", groupId) }
        }
        .decodeList()

    suspend fun searchPayments(query: String): List<Payment> = client.from("payments")
        .select {
            filter {
                or {
                    ilike("reference_number", "%$query%")
                    ilike("notes", "%$query%")
                }
            }
        }
        .decodeList()

    suspend fun getPaymentSummary(): Map<String, Double> {
        val payments = getAllPayments()
        return payments.groupBy { it.status }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }
}

// ─── Teacher Earnings Repository ──────────────────────────────────────────────

class EarningsRepository(private val client: SupabaseClient) {

    suspend fun calculateEarningsForPayment(payment: Payment): TeacherEarning? {
        val group = client.from("groups").select {
            filter { eq("id", payment.group_id) }
        }.decodeSingleOrNull<Group>() ?: return null

        val teacherId = group.teacher_id ?: return null
        val teacher = client.from("teacher_profiles").select {
            filter { eq("user_id", teacherId) }
        }.decodeSingleOrNull<TeacherProfile>() ?: return null

        val percentage = teacher.earnings_percentage
        val earnedAmount = payment.amount * (percentage / 100.0)

        val earning = TeacherEarning(
            id = UUID.randomUUID().toString(),
            teacher_id = teacherId,
            payment_id = payment.id,
            group_id = payment.group_id,
            total_payment_amount = payment.amount,
            percentage_applied = percentage,
            earned_amount = earnedAmount,
            period_start = payment.due_date,
            period_end = payment.due_date,
            status = "pending"
        )

        client.from("teacher_earnings").insert(earning)
        return earning
    }

    suspend fun getTeacherEarnings(teacherId: String): List<TeacherEarning> =
        client.from("teacher_earnings").select {
            filter { eq("teacher_id", teacherId) }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    suspend fun getPendingEarnings(): List<TeacherEarning> =
        client.from("teacher_earnings").select {
            filter { eq("status", "pending") }
        }.decodeList()

    suspend fun getTeacherEarningsTotal(teacherId: String): Double {
        val earnings = getTeacherEarnings(teacherId)
        return earnings.sumOf { it.earned_amount }
    }

    suspend fun getTeacherEarningsByPeriod(teacherId: String, start: String, end: String): List<TeacherEarning> =
        client.from("teacher_earnings").select {
            filter {
                eq("teacher_id", teacherId)
                gte("period_start", start)
                lte("period_end", end)
            }
        }.decodeList()
}

// ─── Teacher Payment Repository ───────────────────────────────────────────────

class TeacherPaymentRepository(private val client: SupabaseClient) {

    suspend fun createTeacherPayment(payment: TeacherPayment): TeacherPayment {
        val id = UUID.randomUUID().toString()
        val newPayment = payment.copy(id = id)
        client.from("teacher_payments").insert(newPayment)
        return newPayment
    }

    suspend fun getTeacherPayments(teacherId: String): List<TeacherPayment> =
        client.from("teacher_payments").select {
            filter { eq("teacher_id", teacherId) }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    suspend fun markTeacherPaymentPaid(paymentId: String, method: String) {
        client.from("teacher_payments").update(
            mapOf(
                "status" to "paid",
                "paid_date" to LocalDate.now().toString(),
                "payment_method" to method
            )
        ) {
            filter { eq("id", paymentId) }
        }
    }

    suspend fun getPendingTeacherPayments(): List<TeacherPayment> =
        client.from("teacher_payments").select {
            filter { eq("status", "pending") }
        }.decodeList()

    suspend fun getUpcomingTeacherPayments(days: Int = 7): List<TeacherPayment> =
        client.from("teacher_payments").select {
            filter {
                eq("status", "pending")
                lte("period_end", LocalDate.now().plusDays(days.toLong()).toString())
            }
        }.decodeList()

    suspend fun scheduleTeacherPayments() {
        val pendingEarnings = EarningsRepository(client).getPendingEarnings()
        val earningsByTeacher = pendingEarnings.groupBy { it.teacher_id }

        for ((teacherId, earnings) in earningsByTeacher) {
            val total = earnings.sumOf { it.earned_amount }
            val start = earnings.minOf { it.period_start }
            val end = earnings.maxOf { it.period_end }

            createTeacherPayment(
                TeacherPayment(
                    id = UUID.randomUUID().toString(),
                    teacher_id = teacherId,
                    total_amount = total,
                    period_start = start,
                    period_end = end,
                    status = "pending"
                )
            )

            // Mark earnings as scheduled
            for (earning in earnings) {
                client.from("teacher_earnings").update(
                    mapOf("status" to "scheduled")
                ) {
                    filter { eq("id", earning.id) }
                }
            }
        }
    }
}

// ─── Financial Repository ─────────────────────────────────────────────────────

class FinancialRepository(private val client: SupabaseClient) {

    suspend fun getAllTransactions(): List<Transaction> = client.from("transactions")
        .select { order("created_at", Order.DESCENDING) }
        .decodeList()

    suspend fun getTransactionsByType(type: String): List<Transaction> = client.from("transactions")
        .select {
            filter {
                eq("type", type)
                eq("is_deleted", false)
            }
            order("created_at", Order.DESCENDING)
        }
        .decodeList()

    suspend fun getTransactionsByPeriod(start: String, end: String): List<Transaction> =
        client.from("transactions").select {
            filter {
                gte("created_at", start)
                lte("created_at", end)
                eq("is_deleted", false)
            }
        }.decodeList()

    suspend fun createTransaction(transaction: Transaction): Transaction {
        val id = UUID.randomUUID().toString()
        val newTxn = transaction.copy(id = id)
        client.from("transactions").insert(newTxn)
        return newTxn
    }

    suspend fun softDeleteTransaction(id: String) {
        client.from("transactions").update(
            mapOf(
                "is_deleted" to true,
                "deleted_at" to LocalDate.now().toString()
            )
        ) {
            filter { eq("id", id) }
        }
    }

    // Never permanently delete financial records
    suspend fun restoreTransaction(id: String) {
        client.from("transactions").update(
            mapOf(
                "is_deleted" to false,
                "deleted_at" to null
            )
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun getAllExpenses(): List<Expense> = client.from("expenses")
        .select { order("date", Order.DESCENDING) }
        .decodeList()

    suspend fun createExpense(expense: Expense): Expense {
        val id = UUID.randomUUID().toString()
        val newExpense = expense.copy(id = id)
        client.from("expenses").insert(newExpense)
        // Record as transaction
        createTransaction(
            Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.EXPENSE.name.lowercase(),
                amount = expense.amount,
                description = "${expense.category}: ${expense.description}",
                reference_id = id,
                reference_type = "expense",
                category = expense.category,
                created_by = expense.created_by
            )
        )
        return newExpense
    }

    suspend fun getTotalRevenue(start: String? = null, end: String? = null): Double {
        val txn = if (start != null && end != null) {
            getTransactionsByPeriod(start, end)
        } else {
            getTransactionsByType("revenue")
        }
        return txn.filter { !it.is_deleted }.sumOf { it.amount }
    }

    suspend fun getTotalExpenses(start: String? = null, end: String? = null): Double {
        val txn = if (start != null && end != null) {
            getTransactionsByPeriod(start, end)
        } else {
            getTransactionsByType("expense")
        }
        return txn.filter { !it.is_deleted }.sumOf { it.amount }
    }

    suspend fun getTotalTeacherPayments(start: String? = null, end: String? = null): Double {
        val txn = if (start != null && end != null) {
            getTransactionsByPeriod(start, end)
        } else {
            getTransactionsByType("teacher_payment")
        }
        return txn.filter { !it.is_deleted }.sumOf { it.amount }
    }

    suspend fun getNetProfit(start: String? = null, end: String? = null): Double {
        return getTotalRevenue(start, end) -
                getTotalExpenses(start, end) -
                getTotalTeacherPayments(start, end)
    }

    suspend fun getRevenueByMonth(): Map<String, Double> {
        val transactions = getTransactionsByType("revenue")
        return transactions.filter { !it.is_deleted }
            .groupBy { it.created_at.substring(0, 7) } // YYYY-MM
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    suspend fun getExpensesByMonth(): Map<String, Double> {
        val transactions = getTransactionsByType("expense")
        return transactions.filter { !it.is_deleted }
            .groupBy { it.created_at.substring(0, 7) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
    }

    suspend fun getFinancialReport(start: String, end: String): FinancialReport {
        val transactions = getTransactionsByPeriod(start, end)
        val activeTransactions = transactions.filter { !it.is_deleted }

        val revenue = activeTransactions.filter { it.type == "revenue" }.sumOf { it.amount }
        val expenses = activeTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        val teacherEarnings = activeTransactions.filter { it.type == "teacher_payment" }.sumOf { it.amount }

        val subs = client.from("subscriptions").select {
            filter {
                gte("created_at", start)
                lte("created_at", end)
            }
        }.decodeList<Subscription>()

        return FinancialReport(
            periodStart = start,
            periodEnd = end,
            totalRevenue = revenue,
            totalExpenses = expenses,
            netProfit = revenue - expenses - teacherEarnings,
            teacherEarnings = teacherEarnings,
            subscriptionCount = subs.size,
            paymentBreakdown = activeTransactions.groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } },
            transactions = activeTransactions
        )
    }
}

// ─── Notification Repository ──────────────────────────────────────────────────

class NotificationRepository(private val client: SupabaseClient) {

    suspend fun getUserNotifications(userId: String): List<AppNotification> =
        client.from("notifications").select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
            limit(50)
        }.decodeList()

    suspend fun markAsRead(notificationId: String) {
        client.from("notifications").update(
            mapOf("is_read" to true)
        ) {
            filter { eq("id", notificationId) }
        }
    }

    suspend fun getUnreadCount(userId: String): Int {
        val notifications = client.from("notifications").select {
            filter {
                eq("user_id", userId)
                eq("is_read", false)
            }
        }.decodeList<AppNotification>()
        return notifications.size
    }

    suspend fun createNotification(notification: AppNotification) {
        client.from("notifications").insert(notification)
    }
}

// ─── Audit Log Repository ─────────────────────────────────────────────────────

class AuditLogRepository(private val client: SupabaseClient) {

    suspend fun logAction(
        userId: String,
        action: String,
        entityType: String,
        entityId: String,
        oldValue: String? = null,
        newValue: String? = null
    ) {
        client.from("audit_logs").insert(
            AuditLog(
                id = UUID.randomUUID().toString(),
                user_id = userId,
                action = action,
                entity_type = entityType,
                entity_id = entityId,
                old_value = oldValue,
                new_value = newValue
            )
        )
    }

    suspend fun getAuditLogs(entityType: String? = null, limit: Int = 100): List<AuditLog> =
        client.from("audit_logs").select {
            if (entityType != null) {
                filter { eq("entity_type", entityType) }
            }
            order("created_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList()
}

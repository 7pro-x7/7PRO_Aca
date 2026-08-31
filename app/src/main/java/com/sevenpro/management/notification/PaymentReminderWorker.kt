package com.sevenpro.management.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.sevenpro.management.MainActivity
import com.sevenpro.management.R
import com.sevenpro.management.SevenProApp
import com.sevenpro.management.data.repository.PaymentRepository
import com.sevenpro.management.data.repository.SubscriptionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class PaymentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val client = createSupabaseClient(
                com.sevenpro.management.BuildConfig.SUPABASE_URL,
                com.sevenpro.management.BuildConfig.SUPABASE_ANON_KEY
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
            }

            val paymentRepo = PaymentRepository(client)
            val subRepo = SubscriptionRepository(client)

            // Check overdue payments
            val overduePayments = paymentRepo.getOverduePayments()
            if (overduePayments.isNotEmpty()) {
                showNotification(
                    title = "Overdue Payments",
                    message = "${overduePayments.size} payment(s) are overdue",
                    channelId = SevenProApp.CHANNEL_PAYMENTS,
                    notificationId = 1001
                )
            }

            // Check upcoming payments (due within 3 days)
            val upcomingPayments = paymentRepo.getUpcomingPayments(3)
            if (upcomingPayments.isNotEmpty()) {
                showNotification(
                    title = "Upcoming Payments",
                    message = "${upcomingPayments.size} payment(s) due in the next 3 days",
                    channelId = SevenProApp.CHANNEL_PAYMENTS,
                    notificationId = 1002
                )
            }

            // Check subscriptions due for renewal
            val today = LocalDate.now().toString()
            val dueSubs = subRepo.getSubscriptionsDueForRenewal(today)
            if (dueSubs.isNotEmpty()) {
                showNotification(
                    title = "Subscription Renewals",
                    message = "${dueSubs.size} subscription(s) need renewal",
                    channelId = SevenProApp.CHANNEL_SUBSCRIPTIONS,
                    notificationId = 1003
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String, channelId: String, notificationId: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<PaymentReminderWorker>(
                8, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "payment_reminders",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

package com.sevenpro.management

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions

class SevenProApp : Application() {

    val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
            install(Storage)
            install(Functions)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val paymentChannel = NotificationChannel(
                CHANNEL_PAYMENTS,
                "Payment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about upcoming, due, and overdue payments"
            }

            val subscriptionChannel = NotificationChannel(
                CHANNEL_SUBSCRIPTIONS,
                "Subscription Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about subscription renewals and changes"
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General application notifications"
            }

            manager.createNotificationChannel(paymentChannel)
            manager.createNotificationChannel(subscriptionChannel)
            manager.createNotificationChannel(generalChannel)
        }
    }

    companion object {
        const val CHANNEL_PAYMENTS = "payments"
        const val CHANNEL_SUBSCRIPTIONS = "subscriptions"
        const val CHANNEL_GENERAL = "general"
    }
}

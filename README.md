# 7PRO Management - Android App

A complete, professional Android application for managing an online educational platform with multiple user roles, subscription billing, teacher earnings tracking, and financial reporting.

## Architecture

```
com.sevenpro.management/
├── SevenProApp.kt                    # Application class (Supabase init, notifications)
├── MainActivity.kt                    # Entry point (theme, locale, splash)
├── data/
│   ├── local/UserPreferences.kt       # DataStore prefs (language, theme, auth)
│   ├── model/Models.kt                # All data models (serializable)
│   └── repository/SupabaseRepository.kt  # All Supabase operations
├── ui/
│   ├── theme/Theme.kt, Type.kt        # Material3 theme (Light/Dark)
│   ├── navigation/Navigation.kt       # Complete nav graph + bottom bar
│   └── screens/
│       ├── auth/                      # Login, Register
│       ├── dashboard/                 # KPI cards, charts, alerts
│       ├── users/                     # List, Detail, Add/Edit
│       ├── groups/                    # List, Add/Edit
│       ├── subscriptions/             # List, Add/Edit
│       ├── payments/                  # List, Detail, Mark-as-Paid
│       ├── earnings/                  # Teacher earnings, payments
│       ├── reports/                   # Financial reports, export
│       ├── settings/                  # Language, theme, logout
│       └── notifications/             # Notification center
├── export/Exporters.kt                # PDF, CSV, Excel export
└── notification/
    ├── PaymentReminderWorker.kt       # Background payment checks
    └── BootReceiver.kt               # Reschedule on boot
```

## Features

- **4 User Roles**: Main Admin, Admin, Teacher, Parent, Student
- **Role-Based Access Control**: Customizable admin permissions
- **Subscription Management**: Monthly billing with auto-renewal
- **Payment Tracking**: Paid, Pending, Overdue, Paused, Cancelled statuses
- **Teacher Earnings**: Individual percentages, automatic calculation
- **Financial Reports**: Revenue, expenses, net profit, teacher payouts
- **Export**: PDF, CSV, Excel reports
- **Notifications**: Overdue payment alerts, subscription renewals
- **Bilingual**: Full Arabic (RTL) and English support
- **Dark/Light Mode**: System-aware with manual toggle
- **Supabase Backend**: Auth, Database, Real-time, Storage
- **CI/CD**: Codemagic automated builds

## Setup

### 1. Supabase

1. Create a project at [supabase.com](https://supabase.com)
2. Go to SQL Editor and run `supabase/setup.sql`
3. Enable Email/Password auth in Authentication > Providers
4. Copy your project URL and anon key from Settings > API

### 2. Android Project

1. Open in Android Studio
2. Create `local.properties`:
```
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```
3. Sync and run

### 3. First Admin Setup

1. Launch the app and register an account
2. In Supabase SQL Editor, run:
```sql
UPDATE user_profiles 
SET role = 'MAIN_ADMIN', 
    permissions = ARRAY['MANAGE_USERS','MANAGE_TEACHERS','MANAGE_STUDENTS',
    'MANAGE_PARENTS','MANAGE_GROUPS','MANAGE_SUBSCRIPTIONS','MANAGE_PAYMENTS',
    'MANAGE_EARNINGS','VIEW_REPORTS','MANAGE_SETTINGS','MANAGE_ADMINS','EXPORT_DATA']
WHERE email = 'your-email@example.com';
```
3. Sign out and sign back in

### 4. Codemagic CI/CD

1. Connect your GitHub repo to [Codemagic](https://codemagic.io)
2. In Codemagic Settings > Environment Groups, create `sevenpro_credentials`:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
   - `KEYSTORE_BASE64` (run `base64 -i release-key.jks`)
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
   - `PLAY_STORE_JSON_KEY` (for auto-deploy)
3. Push to `main` to trigger production build
4. Push to `develop` for beta builds

## Database Schema

The Supabase SQL script creates 14 tables with:
- **Row Level Security (RLS)** on every table
- **Triggers** for auto-profile creation and payment renewal
- **Indexes** for performance
- **Audit logging** for financial records
- **Soft-delete** for transactions (never permanently delete)

## Key Design Decisions

1. **Teacher percentage changes don't affect historical records** - Each `teacher_earning` record stores the `percentage_applied` at the time of calculation
2. **Financial records are never permanently deleted** - Uses soft-delete (`is_deleted` flag) with audit trail
3. **Automatic payment renewal** - When a payment is marked paid, the next month's payment is auto-created
4. **Notification scheduling** - WorkManager checks every 8 hours for overdue payments and upcoming renewals
5. **Offline-aware** - Supabase client handles reconnection automatically

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Backend**: Supabase (Auth, Postgreal, Realtime)
- **Local Storage**: DataStore Preferences
- **Background**: WorkManager
- **Export**: iText7 (PDF), OpenCSV, Apache POI (Excel)
- **DI**: Manual (no Hilt for simplicity)
- **CI/CD**: Codemagic

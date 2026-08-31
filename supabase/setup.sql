-- ============================================================
-- 7PRO Management Platform - Supabase Database Schema
-- Run this in Supabase SQL Editor to set up the database
-- ============================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- 1. USER PROFILES
-- ============================================================
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL DEFAULT '',
    full_name TEXT NOT NULL DEFAULT '',
    role TEXT NOT NULL DEFAULT 'STUDENT' CHECK (role IN ('MAIN_ADMIN', 'ADMIN', 'TEACHER', 'PARENT', 'STUDENT')),
    phone TEXT DEFAULT '',
    is_active BOOLEAN DEFAULT true,
    permissions TEXT[] DEFAULT '{}',
    avatar_url TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Auto-create profile on signup
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_profiles (id, email, full_name, role)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'full_name', ''),
        COALESCE(NEW.raw_user_meta_data->>'role', 'STUDENT')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION handle_new_user();

-- ============================================================
-- 2. TEACHER PROFILES
-- ============================================================
CREATE TABLE teacher_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    earnings_percentage NUMERIC(5,2) DEFAULT 25.00,
    custom_percentage BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    subjects TEXT[] DEFAULT '{}',
    bio TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_teacher_profiles_user_id ON teacher_profiles(user_id);

-- ============================================================
-- 3. PARENT PROFILES
-- ============================================================
CREATE TABLE parent_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    children_ids UUID[] DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================
-- 4. STUDENT PROFILES
-- ============================================================
CREATE TABLE student_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES parent_profiles(id),
    grade_level TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================
-- 5. GROUPS
-- ============================================================
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    name_ar TEXT DEFAULT '',
    subject TEXT NOT NULL DEFAULT '',
    teacher_id UUID REFERENCES user_profiles(id),
    capacity INTEGER DEFAULT 20,
    schedule TEXT DEFAULT '',
    monthly_fee NUMERIC(10,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_groups_teacher_id ON groups(teacher_id);
CREATE INDEX idx_groups_is_active ON groups(is_active);

-- ============================================================
-- 6. GROUP STUDENTS (Many-to-Many)
-- ============================================================
CREATE TABLE group_students (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(group_id, student_id)
);

-- ============================================================
-- 7. SUBSCRIPTIONS
-- ============================================================
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    parent_id UUID NOT NULL REFERENCES user_profiles(id),
    student_id UUID NOT NULL REFERENCES user_profiles(id),
    group_id UUID NOT NULL REFERENCES groups(id),
    status TEXT DEFAULT 'active' CHECK (status IN ('active', 'paused', 'cancelled', 'expired')),
    monthly_fee NUMERIC(10,2) DEFAULT 0.00,
    start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    next_billing_date DATE NOT NULL,
    last_payment_date DATE,
    auto_renew BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_next_billing ON subscriptions(next_billing_date);
CREATE INDEX idx_subscriptions_parent_id ON subscriptions(parent_id);

-- ============================================================
-- 8. PAYMENTS
-- ============================================================
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_id UUID NOT NULL REFERENCES subscriptions(id),
    parent_id UUID NOT NULL REFERENCES user_profiles(id),
    student_id UUID NOT NULL REFERENCES user_profiles(id),
    group_id UUID NOT NULL REFERENCES groups(id),
    amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    status TEXT DEFAULT 'pending' CHECK (status IN ('paid', 'pending', 'overdue', 'partial', 'refunded')),
    due_date DATE NOT NULL,
    paid_date DATE,
    payment_method TEXT,
    reference_number TEXT,
    notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_due_date ON payments(due_date);
CREATE INDEX idx_payments_parent_id ON payments(parent_id);

-- ============================================================
-- 9. TEACHER EARNINGS
-- ============================================================
CREATE TABLE teacher_earnings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID NOT NULL REFERENCES user_profiles(id),
    payment_id UUID NOT NULL REFERENCES payments(id),
    group_id UUID NOT NULL REFERENCES groups(id),
    total_payment_amount NUMERIC(10,2) DEFAULT 0.00,
    percentage_applied NUMERIC(5,2) DEFAULT 0.00,
    earned_amount NUMERIC(10,2) DEFAULT 0.00,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'scheduled', 'paid')),
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_teacher_earnings_teacher_id ON teacher_earnings(teacher_id);
CREATE INDEX idx_teacher_earnings_status ON teacher_earnings(status);

-- ============================================================
-- 10. TEACHER PAYMENTS
-- ============================================================
CREATE TABLE teacher_payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID NOT NULL REFERENCES user_profiles(id),
    total_amount NUMERIC(10,2) DEFAULT 0.00,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status TEXT DEFAULT 'pending' CHECK (status IN ('pending', 'paid', 'processing')),
    paid_date DATE,
    payment_method TEXT,
    notes TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_teacher_payments_teacher_id ON teacher_payments(teacher_id);
CREATE INDEX idx_teacher_payments_status ON teacher_payments(status);

-- ============================================================
-- 11. TRANSACTIONS (Financial Ledger - never permanently delete)
-- ============================================================
CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type TEXT NOT NULL CHECK (type IN ('revenue', 'expense', 'teacher_payment', 'refund', 'other')),
    amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    description TEXT DEFAULT '',
    reference_id UUID,
    reference_type TEXT,
    category TEXT DEFAULT '',
    created_by UUID REFERENCES user_profiles(id),
    created_at TIMESTAMPTZ DEFAULT now(),
    is_deleted BOOLEAN DEFAULT false,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_is_deleted ON transactions(is_deleted);

-- ============================================================
-- 12. EXPENSES
-- ============================================================
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category TEXT NOT NULL DEFAULT '',
    description TEXT DEFAULT '',
    amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    receipt_url TEXT,
    created_by UUID REFERENCES user_profiles(id),
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ============================================================
-- 13. AUDIT LOG (never delete)
-- ============================================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_profiles(id),
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id UUID,
    old_value JSONB,
    new_value JSONB,
    ip_address TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- ============================================================
-- 14. NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES user_profiles(id),
    title TEXT NOT NULL DEFAULT '',
    title_ar TEXT DEFAULT '',
    message TEXT DEFAULT '',
    message_ar TEXT DEFAULT '',
    type TEXT DEFAULT 'general',
    is_read BOOLEAN DEFAULT false,
    reference_id UUID,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);

-- ============================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================

ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE parent_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE student_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_students ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_earnings ENABLE ROW LEVEL SECURITY;
ALTER TABLE teacher_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

-- Admin/Main Admin: Full access to everything
CREATE POLICY "Admins full access on user_profiles" ON user_profiles
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM user_profiles
            WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN')
        )
    );

CREATE POLICY "Users can view their own profile" ON user_profiles
    FOR SELECT USING (id = auth.uid());

-- Teachers can view their own data
CREATE POLICY "Teachers view own data" ON teacher_profiles
    FOR SELECT USING (
        user_id = auth.uid() OR
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

-- Admins manage teacher profiles
CREATE POLICY "Admins manage teachers" ON teacher_profiles
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

-- Groups: everyone can view, admins manage
CREATE POLICY "Groups viewable by authenticated" ON groups
    FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Admins manage groups" ON groups
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

-- Subscriptions: admins manage, parents view their own
CREATE POLICY "Admins manage subscriptions" ON subscriptions
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

CREATE POLICY "Parents view own subscriptions" ON subscriptions
    FOR SELECT USING (parent_id = auth.uid());

-- Payments: admins manage
CREATE POLICY "Admins manage payments" ON payments
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

CREATE POLICY "Parents view own payments" ON payments
    FOR SELECT USING (parent_id = auth.uid());

-- Teacher Earnings: admins + teachers view own
CREATE POLICY "Admins manage teacher earnings" ON teacher_earnings
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

CREATE POLICY "Teachers view own earnings" ON teacher_earnings
    FOR SELECT USING (teacher_id = auth.uid());

-- Teacher Payments: admins + teachers view own
CREATE POLICY "Admins manage teacher payments" ON teacher_payments
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

CREATE POLICY "Teachers view own payments" ON teacher_payments
    FOR SELECT USING (teacher_id = auth.uid());

-- Transactions: admins manage, view only for others
CREATE POLICY "Admins manage transactions" ON transactions
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

-- Expenses: admins manage
CREATE POLICY "Admins manage expenses" ON expenses
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

-- Audit logs: admins view only
CREATE POLICY "Admins view audit logs" ON audit_logs
    FOR SELECT USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

CREATE POLICY "System inserts audit logs" ON audit_logs
    FOR INSERT WITH CHECK (true);

-- Notifications: users view their own
CREATE POLICY "Users view own notifications" ON notifications
    FOR SELECT USING (user_id = auth.uid());

CREATE POLICY "Users update own notifications" ON notifications
    FOR UPDATE USING (user_id = auth.uid());

CREATE POLICY "System inserts notifications" ON notifications
    FOR INSERT WITH CHECK (true);

-- Group students: admins manage
CREATE POLICY "Admins manage group_students" ON group_students
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

CREATE POLICY "Authenticated view group_students" ON group_students
    FOR SELECT USING (auth.role() = 'authenticated');

-- Parent profiles: admins manage
CREATE POLICY "Admins manage parent_profiles" ON parent_profiles
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

-- Student profiles: admins manage
CREATE POLICY "Admins manage student_profiles" ON student_profiles
    FOR ALL USING (
        EXISTS (SELECT 1 FROM user_profiles WHERE id = auth.uid() AND role IN ('MAIN_ADMIN', 'ADMIN'))
    );

-- ============================================================
-- DATABASE FUNCTIONS FOR TRIGGERS
-- ============================================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_user_profiles_updated_at
    BEFORE UPDATE ON user_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER update_groups_updated_at
    BEFORE UPDATE ON groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================================
-- FUNCTION: Create payment records when subscription is created
-- ============================================================
CREATE OR REPLACE FUNCTION create_initial_payment()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO payments (subscription_id, parent_id, student_id, group_id, amount, status, due_date, notes)
    VALUES (
        NEW.id,
        NEW.parent_id,
        NEW.student_id,
        NEW.group_id,
        NEW.monthly_fee,
        'pending',
        NEW.next_billing_date,
        'Monthly subscription payment'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_subscription_created
    AFTER INSERT ON subscriptions
    FOR EACH ROW EXECUTE FUNCTION create_initial_payment();

-- ============================================================
-- FUNCTION: Auto-renew subscriptions
-- ============================================================
CREATE OR REPLACE FUNCTION renew_subscription()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'paid' AND OLD.status = 'pending' THEN
        -- Create next payment
        INSERT INTO payments (subscription_id, parent_id, student_id, group_id, amount, status, due_date, notes)
        SELECT
            s.id,
            s.parent_id,
            s.student_id,
            s.group_id,
            s.monthly_fee,
            'pending',
            s.next_billing_date + INTERVAL '1 month',
            'Monthly subscription payment'
        FROM subscriptions s
        WHERE s.id = NEW.subscription_id AND s.auto_renew = true AND s.status = 'active';

        -- Update next billing date
        UPDATE subscriptions
        SET next_billing_date = next_billing_date + INTERVAL '1 month',
            last_payment_date = CURRENT_DATE
        WHERE id = NEW.subscription_id AND auto_renew = true AND status = 'active';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_payment_updated
    AFTER UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION renew_subscription();

-- ============================================================
-- SEED DATA: Create the first Main Admin user
-- Note: First sign up through the app, then run this to make them Main Admin
-- ============================================================
-- UPDATE user_profiles SET role = 'MAIN_ADMIN', permissions = ARRAY['MANAGE_USERS','MANAGE_TEACHERS','MANAGE_STUDENTS','MANAGE_PARENTS','MANAGE_GROUPS','MANAGE_SUBSCRIPTIONS','MANAGE_PAYMENTS','MANAGE_EARNINGS','VIEW_REPORTS','MANAGE_SETTINGS','MANAGE_ADMINS','EXPORT_DATA']
-- WHERE email = 'your-email@example.com';

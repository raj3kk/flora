-- Flora core tables — Cycle 1 FIX (S1-14).
-- Source: docs/admin-phase-a-tables.sql (payment_submissions +
-- admin_audit wahi se liye gaye hain) + live functions ke
-- select/insert fields (users, licences, usage, plans, settings).
--
-- !!!! PEHLE PADHO — YE FILE TABHI CHALAO JAB NEECHE KE 8
-- DEPLOY-CHECKS LIVE PAR VERIFY HO JAYEIN !!!!
-- Ye file live deploy-check ke baad hi chalani hai. Bina verify
-- kiye chalane se maujooda live schema se takrao ho sakta hai.
-- Deploy-checks (live dashboard/SQL se confirm karo):
--   1. licences.device_id nullability — website-path NULL-claim
--      rows ke liye NULL-able hona chahiye; NOT NULL ho to pehle
--      ALTER TABLE public.licences ALTER COLUMN device_id
--      DROP NOT NULL (ek baar, dashboard se) — ye file us
--      nullability ko NAHI chhedti.
--   2. (user_id, device_id) unique — admin-approve app-path
--      upsert (onConflict: "user_id,device_id") isi par nirbhar
--      hai; live me duplicate bound rows hon to unique index
--      fail hoga (loud failure) — pehle duplicates saaf karo.
--   3. users.device_id nullability — app-link se pehle users
--      row bina device ke banti hai; NOT NULL ho to link flow
--      tootega — ye file is column ko NAHI chhedti.
--   4. payment_submissions INSERT policy — user apni row bana
--      sake (auth.uid() = user_id); policy galat ho to pay page
--      se UTR submit fail hoga.
--   5. admin_audit policies — admin select + admin insert hi
--      honi chahiye, koi update/delete policy nahi (append-only);
--      galat policy audit trail ko badal sakti hai.
--   6. verify_jwt settings — functions ke JWT gate (anon client
--      auth.getUser) ke liye Supabase Auth settings sahi honi
--      chahiye; galat settings par har admin call 401 dega.
--   7. plans schema — live plans table ke columns confirm karo
--      (licence plan_id plans.id ko refer karta hai); jo confirm
--      nahi hai use yahan guess NAHI kiya gaya hai — neeche
--      plans table sirf tab banti hai jab wo maujood hi na ho,
--      aur maujooda table ko ye file bilkul NAHI badalti.
--   8. settings schema — admin_settings (key/value) + global_
--      instructions / feature_flags / provider_keys ke live
--      columns confirm karo; maujooda tables par ye file sirf
--      missing columns jodti hai, maujooda columns NAHI badalti.
--   9. push_tokens columns (S1-39, Cycle 2 FIX) — live columns
--      deploy-check se CONFIRM karo: push-token-sync upsert
--      (user_id, device_id, fcm_token, platform) + admin-list
--      select (user_id, updated_at) par nirbhar hai; device_id
--      ka type (yahan uuid, users.device_id jaisa) app se aane
--      wale value se match hona chahiye, warna upsert fail hoga.
--      Unique (user_id, device_id) tabhi banega jab duplicate
--      rows na hon (hon to pehle saaf karo — loud failure).
--
-- IDEMPOTENT: har statement IF NOT EXISTS / DO-block guard ke
-- saath hai — live par dobara chalane se duplicate-policy ya
-- duplicate-table error NAHI aayega (yehi Phase A file ki kami
-- thi: uski policy-create steps dobara chalane par error deti
-- hain, isliye wo file dobara mat chalao — ye file chalao).
-- Koi key/secret is file me nahi hai. Koi live schema guess
-- nahi kiya gaya hai — jo confirm nahi hai, wo upar
-- "deploy-check" me likha hai, code me guess nahi bhara gaya.
--
-- Run order: pehle 8 deploy-checks verify karo, phir ye file
-- (ek baar), phir functions deploy karo.

-- --- users (app-link + admin check ki base table) ---
create table if not exists public.users (
  id uuid primary key,
  device_id uuid,
  is_admin boolean not null default false,
  created_at timestamptz not null default now()
);
-- Missing columns sirf tab jodo (maujooda table ko mat chhedo).
do $$
begin
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'users'
    and column_name = 'device_id') then
    alter table public.users add column device_id uuid;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'users'
    and column_name = 'is_admin') then
    alter table public.users add column is_admin boolean not null default false;
  end if;
end $$;

-- --- licences (plan/device/status/key — LFMS columns bhi) ---
create table if not exists public.licences (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null,
  device_id uuid,
  plan_id text not null default 'free',
  status text not null default 'active'
    check (status in ('active', 'revoked', 'expired', 'superseded')),
  licence_key text,
  activated_at timestamptz,
  expires_at timestamptz,
  revoked_reason text,
  revoked_by uuid,
  revoked_at timestamptz,
  superseded_by uuid,
  created_at timestamptz not null default now()
);
do $$
begin
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'licence_key') then
    alter table public.licences add column licence_key text;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'revoked_reason') then
    alter table public.licences add column revoked_reason text;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'revoked_by') then
    alter table public.licences add column revoked_by uuid;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'revoked_at') then
    alter table public.licences add column revoked_at timestamptz;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'licences'
    and column_name = 'superseded_by') then
    alter table public.licences add column superseded_by uuid;
  end if;
end $$;
-- Unique indexes (NULL device rows aapas me nahi takrati —
-- Postgres unique index me NULL alag-alag maane jaate hain).
-- Deploy-check #2: duplicate bound rows hon to ye fail hoga.
create unique index if not exists licences_licence_key_uidx
  on public.licences (licence_key);
create unique index if not exists licences_user_device_uidx
  on public.licences (user_id, device_id);

-- --- usage (usage-count + admin-list/admin-overview fields) ---
-- day text hai (YYYY-MM-DD, UTC day — usage-count function
-- isi format me likhta/padhta hai).
create table if not exists public.usage (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null,
  day text not null,
  talk_seconds integer not null default 0,
  tool_calls integer not null default 0,
  created_at timestamptz not null default now()
);
create index if not exists usage_user_day_idx
  on public.usage (user_id, day);

-- --- plans (deploy-check #7 — sirf nayi table par banta hai) ---
create table if not exists public.plans (
  id text primary key,
  display_name text,
  price_paisa integer,
  updated_at timestamptz not null default now()
);

-- --- settings: admin_settings (pay page UPI + panel UPI) ---
create table if not exists public.admin_settings (
  key text primary key,
  value text not null,
  updated_at timestamptz not null default now()
);

-- --- payment_submissions (Phase A source se, idempotent) ---
create table if not exists public.payment_submissions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null,
  amount_paisa integer not null,
  utr text not null,
  note text not null default '',
  status text not null default 'pending'
    check (status in ('pending', 'approved', 'rejected')),
  reviewed_by uuid,
  reviewed_at timestamptz,
  reject_reason text,
  created_at timestamptz not null default now()
);

-- --- admin_audit (append-only — koi update/delete policy nahi) ---
create table if not exists public.admin_audit (
  id uuid primary key default gen_random_uuid(),
  admin_user_id uuid not null,
  action text not null,
  target_user_id uuid,
  before_json jsonb,
  after_json jsonb,
  created_at timestamptz not null default now()
);

alter table public.payment_submissions enable row level security;
alter table public.admin_audit enable row level security;

-- Policies IDEMPOTENT tareeke se (pg_policies check ke saath —
-- dobara chalane par duplicate-policy error nahi aayega).
do $$
begin
  if not exists (select 1 from pg_policies
    where schemaname = 'public' and tablename = 'payment_submissions'
    and policyname = 'user can create own payment submission') then
    create policy "user can create own payment submission"
      on public.payment_submissions for insert
      with check (auth.uid() = user_id);
  end if;
  if not exists (select 1 from pg_policies
    where schemaname = 'public' and tablename = 'payment_submissions'
    and policyname = 'user can read own payment submissions') then
    create policy "user can read own payment submissions"
      on public.payment_submissions for select
      using (auth.uid() = user_id);
  end if;
  if not exists (select 1 from pg_policies
    where schemaname = 'public' and tablename = 'payment_submissions'
    and policyname = 'admin can read all payment submissions') then
    create policy "admin can read all payment submissions"
      on public.payment_submissions for select
      using (exists (
        select 1 from public.users
        where users.id = auth.uid() and users.is_admin = true
      ));
  end if;
  if not exists (select 1 from pg_policies
    where schemaname = 'public' and tablename = 'payment_submissions'
    and policyname = 'admin can review payment submissions') then
    create policy "admin can review payment submissions"
      on public.payment_submissions for update
      using (exists (
        select 1 from public.users
        where users.id = auth.uid() and users.is_admin = true
      ));
  end if;
  if not exists (select 1 from pg_policies
    where schemaname = 'public' and tablename = 'admin_audit'
    and policyname = 'admin can read audit log') then
    create policy "admin can read audit log"
      on public.admin_audit for select
      using (exists (
        select 1 from public.users
        where users.id = auth.uid() and users.is_admin = true
      ));
  end if;
  if not exists (select 1 from pg_policies
    where schemaname = 'public' and tablename = 'admin_audit'
    and policyname = 'admin can insert audit log') then
    create policy "admin can insert audit log"
      on public.admin_audit for insert
      with check (auth.uid() = admin_user_id and exists (
        select 1 from public.users
        where users.id = auth.uid() and users.is_admin = true
      ));
  end if;
end $$;
-- admin_audit par koi update/delete policy nahi hai = append-only.
-- NOTE: users / licences / usage / plans / admin_settings par
-- is file me RLS enable NAHI kiya gaya hai — unki live RLS
-- sthiti deploy-check me confirm karo (functions service-role
-- se chalte hain, isliye RLS unhe nahi rokta; client-side reads
-- par RLS ka asar deploy-check ke baad hi tay karo).

-- --- push_tokens (S1-39, Cycle 2 FIX) ---
-- push-token-sync upsert (user_id, device_id, fcm_token,
-- platform; onConflict user_id,device_id) + admin-list select
-- (user_id, updated_at) isi table par nirbhar hain — iske bina
-- push-token sync 500 dega aur admin-list push presence khaali
-- rahega. push-token-sync FUNCTION ko mat chhuo (LFMS-reserved)
-- — sirf ye SQL definition free hai. Idempotent: CREATE IF NOT
-- EXISTS + missing-column guards + unique index IF NOT EXISTS.
-- Live columns deploy-check #9 se confirm karo (upar).
create table if not exists public.push_tokens (
  user_id uuid not null,
  device_id uuid,
  fcm_token text not null,
  platform text not null default 'android',
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
do $$
begin
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'push_tokens'
    and column_name = 'device_id') then
    alter table public.push_tokens add column device_id uuid;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'push_tokens'
    and column_name = 'fcm_token') then
    alter table public.push_tokens add column fcm_token text;
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'push_tokens'
    and column_name = 'platform') then
    alter table public.push_tokens add column platform text not null default 'android';
  end if;
  if not exists (select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'push_tokens'
    and column_name = 'updated_at') then
    alter table public.push_tokens add column updated_at timestamptz not null default now();
  end if;
end $$;
-- Unique (user_id, device_id): push-token-sync ka onConflict
-- isi par nirbhar hai. NULL device_id wali rows aapas me nahi
-- takrati (Postgres NULL rule) — device wali rows par hi unique
-- lagu hota hai.
create unique index if not exists push_tokens_user_device_uidx
  on public.push_tokens (user_id, device_id);
